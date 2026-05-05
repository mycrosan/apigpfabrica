package br.compneusgppremium.api.controller.model;


import org.springframework.security.core.GrantedAuthority;

import javax.persistence.*;

@Entity(name = "perfil")

public class PerfilModel implements GrantedAuthority {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String descricao;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    @Override
    public String getAuthority() {
        return descricao;
    }
}
