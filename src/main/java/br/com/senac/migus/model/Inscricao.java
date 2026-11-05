package br.com.senac.migus.model;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.FieldType;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.util.Date;

@Document(collection = "inscricoes")
public class Inscricao {
    @MongoId(FieldType.OBJECT_ID)
    private String id;
    @Field(targetType = FieldType.OBJECT_ID)
    private String voluntarioId;
    @Field(targetType = FieldType.OBJECT_ID)
    private String programaId;
    private String status;
    private Date dataInscricao;
    private Date dataAprovacao;
    private Date dataRecusa;
    private Date dataAtualizacao;

    public Inscricao(String id, String voluntarioId, String programaId, String status, Date dataInscricao,
                     Date dataAprovacao, Date dataRecusa, Date dataAtualizacao) {
        this.id = id;
        this.voluntarioId = voluntarioId;
        this.programaId = programaId;
        this.status = status;
        this.dataInscricao = dataInscricao;
        this.dataAprovacao = dataAprovacao;
        this.dataRecusa = dataRecusa;
        this.dataAtualizacao = dataAtualizacao;
    }

    public String getId() {
        return id;
    }
    public String getVoluntarioId() {
        return voluntarioId;
    }
    public String getProgramaId() {
        return programaId;
    }
    public String getStatus() {
        return status;
    }
    public Date getDataInscricao() {
        return dataInscricao;
    }
    public Date getDataAprovacao() {
        return dataAprovacao;
    }
    public Date getDataRecusa() {
        return dataRecusa;
    }
    public Date getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void aprovar(Date data) {
        this.status = "APROVADA";
        this.dataAprovacao = data;
        this.dataAtualizacao = data;
    }

    public void recusar(Date data) {
        this.status = "RECUSADA";
        this.dataRecusa = data;
        this.dataAtualizacao = data;
    }
}
