package com.ms_financeiro.repository;

import com.ms_financeiro.model.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUsuarioId(Long usuarioId);
    Address findByEndereco(String endereco);
}
