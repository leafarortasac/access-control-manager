package br.com.access_control_manager.application.dto;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class UserImportCsvDTO {

    @CsvBindByName(column = "nome_completo")
    private String fullName;

    @CsvBindByName(column = "email")
    private String email;

    @CsvBindByName(column = "cpf")
    private String cpf;

    @CsvBindByName(column = "telefone")
    private String phone;

    @CsvBindByName(column = "perfil")
    private String role;
}