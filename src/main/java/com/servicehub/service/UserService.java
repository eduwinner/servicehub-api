package com.servicehub.service;

import com.servicehub.exception.EmailAlreadyExistsException;
import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.User;
import com.servicehub.model.dto.UserDTO;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.model.dto.UserUpdateDTO;
import com.servicehub.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(UserDTO dto) {
        String email = normalizeEmail(dto.getEmail());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(email);
        user.setPasswordHash(dto.getPassword()); // depois trocar por hash

        return toResponseDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        return toResponseDTO(findEntityById(id));
    }

    public UserResponseDTO updateUser(Long id, UserUpdateDTO dto) {
        User user = findEntityById(id);
        user.setFullName(dto.fullName());
        user.setPhone(dto.phone());
        user.setBio(dto.bio());
        user.setAvatarUrl(dto.avatarUrl());
        return toResponseDTO(userRepository.saveAndFlush(user));
    }

    public void deleteUser(Long id) {
        User user = findEntityById(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public Page<UserResponseDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponseDTO);
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + id));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(),
                user.getBio(), user.getAvatarUrl(), user.getCreatedAt(), user.getUpdatedAt());
    }

}
