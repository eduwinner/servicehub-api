package com.servicehub.service;

import com.servicehub.exception.BusinessRuleException;
import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.model.RequestStatus;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.Role;
import com.servicehub.model.User;
import com.servicehub.model.dto.RegisterDTO;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.model.dto.UserUpdateDTO;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.ServiceRequestRepository;
import com.servicehub.repository.UserRepository;
import com.servicehub.security.CurrentUser;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ServiceOfferingRepository serviceOfferingRepository;
    private final CurrentUser currentUser;
    private final ServiceRequestRepository requestRepository;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       ServiceOfferingRepository serviceOfferingRepository, CurrentUser currentUser,
                       ServiceRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.serviceOfferingRepository = serviceOfferingRepository;
        this.currentUser = currentUser;
    }

    /** Cadastro público (RN01, RN02, RN03). O papel vem validado no DTO: só CLIENT ou PROVIDER. */
    public UserResponseDTO register(RegisterDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.valueOf(dto.getRole()));
        try {
            return toResponseDTO(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // duas requisições simultâneas passaram pelo existsByEmail; a UNIQUE do banco decide (RN01)
            throw new EmailAlreadyExistsException(email);
        }
    }

    /** RN13: o próprio usuário ou um ADMIN. A permissão é checada antes da existência. */
    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        requireSelfOrAdmin(id);
        return toResponseDTO(findEntityById(id));
    }

    public UserResponseDTO updateUser(Long id, UserUpdateDTO dto) {
        requireSelfOrAdmin(id);
        User user = findEntityById(id);
        user.setFullName(dto.fullName());
        user.setPhone(dto.phone() == null ? null : dto.phone().trim());
        user.setBio(TextSanitizer.clean(dto.bio()));
        user.setAvatarUrl(dto.avatarUrl() == null ? null : dto.avatarUrl().trim());
        return toResponseDTO(userRepository.saveAndFlush(user));
    }

    /** Exclusão lógica (RN11): a conta deixa de logar e os serviços do usuário saem do catálogo. */
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteUser(Long id) {
        changeActive(id, false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO changeRole(Long id, Role role) {
        User user = findEntityById(id);
        if (role != Role.ADMIN) {
            requireNotLockingOutAdmins(user);
        }
        user.setRole(role);
        return toResponseDTO(userRepository.saveAndFlush(user));
    }

    /** Reativar a conta não reativa os serviços: o prestador decide quais voltam ao catálogo. */
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponseDTO changeActive(Long id, boolean active) {
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
        if (!active) {
            requireNotLockingOutAdmins(user);
            serviceOfferingRepository.lockAllByProvider(id); // fila com contratações em andamento (RN10)
        }
        if (!active && (requestRepository.existsByClient_IdAndStatusIn(id, RequestStatus.ACTIVE)
                || requestRepository.existsByService_Provider_IdAndStatusIn(id, RequestStatus.ACTIVE))) {
            throw new BusinessRuleException("O usuário tem ordens em andamento (como cliente ou prestador). "
                    + "Conclua ou cancele as ordens antes de desativá-lo.");
        }
        user.setActive(active);
        if (!active) {
            serviceOfferingRepository.deactivateAllByProvider(id);
        }
        return toResponseDTO(userRepository.saveAndFlush(user));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponseDTO);
    }

    /** Ninguém desativa ou rebaixa a si mesmo, e a plataforma nunca fica sem ADMIN ativo. */
    private void requireNotLockingOutAdmins(User target) {
        if (target.getId().equals(currentUser.get().id())) {
            throw new BusinessRuleException("Você não pode desativar nem rebaixar a sua própria conta.");
        }
        if (target.getRole() == Role.ADMIN && target.isActive()
                && userRepository.countByRoleAndActiveTrue(Role.ADMIN) <= 1) {
            throw new BusinessRuleException("A plataforma precisa de ao menos um ADMIN ativo.");
        }
    }

    private void requireSelfOrAdmin(Long id) {
        var me = currentUser.get();
        if (!me.isAdmin() && !me.id().equals(id)) {
            throw new AccessDeniedException("Você só pode acessar o seu próprio perfil.");
        }
    }

    User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getFullName(), user.getEmail(), user.getRole(),
                user.isActive(), user.getPhone(), user.getBio(), user.getAvatarUrl(),
                user.getCreatedAt(), user.getUpdatedAt());
    }
}
