package br.com.senac.migus.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;
import java.util.List;

@Document(collection = "programas")
public class Programa {
    @MongoId(FieldType.OBJECT_ID)
    private String id;
    private String nome;
    private String descricao;
    private List<String> categoria;
    private List<String> diasDisponiveis;
    private String horarioInicio;
    private String horarioFim;
    private Boolean ativo;
    @Field(targetType = FieldType.OBJECT_ID)
    private String responsavelId;
    private Date dataCadastro;
    private Date dataAtualizacao;

    public Programa(String id, String nome, String descricao, List<String> categoria,
                    List<String> diasDisponiveis, String horarioInicio, String horarioFim,
                    Boolean ativo, String responsavelId, Date dataCadastro, Date dataAtualizacao) {
        this.id = id;
        this.nome = nome;
        this.descricao = descricao;
        this.categoria = categoria;
        this.diasDisponiveis = diasDisponiveis;
        this.horarioInicio = horarioInicio;
        this.horarioFim = horarioFim;
        this.ativo = ativo;
        this.responsavelId = responsavelId;
        this.dataCadastro = dataCadastro;
        this.dataAtualizacao = dataAtualizacao;
    }

    public String getId() {
        return id;
    }
    public String getNome() {
        return nome;
    }
    public String getDescricao() {
        return descricao;
    }
    public List<String> getCategoria() {
        return categoria;
    }
    public List<String> getDiasDisponiveis() {
        return diasDisponiveis;
    }
    public String getHorarioInicio() {
        return horarioInicio;
    }
    public String getHorarioFim() {
        return horarioFim;
    }
    public Boolean getAtivo() {
        return ativo;
    }
    public String getResponsavelId() {
        return responsavelId;
    }
    public Date getDataCadastro() {
        return dataCadastro;
    }
    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }
}
