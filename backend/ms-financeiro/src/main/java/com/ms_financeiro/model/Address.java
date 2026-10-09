package com.ms_financeiro.model;

import jakarta.persistence.*;

@Entity
@Table(name = "enderecos")
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String endereco;

    public Address() {}

    public Address(Long usuarioId, String nome, String endereco) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.endereco = endereco;
    }

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
}
