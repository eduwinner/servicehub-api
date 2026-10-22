package com.servicehub.service;

import com.servicehub.model.User;
import com.servicehub.model.dto.UserDTO;
import com.servicehub.model.dto.UserResponseDTO;
import com.servicehub.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponseDTO createUser(UserDTO dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("E-mail já cadastrado.");
        }

        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(dto.getPassword()); // depois trocar por hash

        return toResponseDTO(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(Long id) {
        return toResponseDTO(findEntityById(id));
    }

    public UserResponseDTO updateUser(Long id, UserDTO dto) {
        User user = findEntityById(id);
        user.setFullName(dto.getFullName());
        return toResponseDTO(userRepository.save(user));
    }

    public void deleteUser(Long id) {
        User user = findEntityById(id);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private User findEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    private UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getFullName(), user.getEmail(), user.getCreatedAt());
    }

}
