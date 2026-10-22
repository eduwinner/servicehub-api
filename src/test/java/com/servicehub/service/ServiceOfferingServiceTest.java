package com.servicehub.service;

import com.servicehub.exception.ResourceNotFoundException;
import com.servicehub.model.ServiceCategory;
import com.servicehub.model.dto.ServiceOfferingDTO;
import com.servicehub.repository.ServiceOfferingRepository;
import com.servicehub.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceOfferingServiceTest {

    @Mock
    ServiceOfferingRepository serviceRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ServiceOfferingService service;

    @Test
    @DisplayName("deve lançar ResourceNotFound quando o prestador não existe")
    void deveLancarResourceNotFoundQuandoPrestadorInexistente() {
        ServiceOfferingDTO dto = new ServiceOfferingDTO();
        dto.setTitle("Instalação elétrica");
        dto.setPrice(new BigDecimal("150.00"));
        dto.setCategory(ServiceCategory.ELETRICA);
        dto.setProviderId(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createService(dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("7");
    }

    @Test
    @DisplayName("deve lançar ResourceNotFound quando o serviço não existe")
    void deveLancarResourceNotFoundQuandoServicoInexistente() {
        when(serviceRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(42L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("42");
    }
}
