package br.com.gerenciador.controller;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Datas no formato brasileiro, usadas nas telas como ${@formato.data(...)}.
 * Datas com hora vêm do banco em UTC e são mostradas no fuso do servidor.
 */
@Component("formato")
public class FormatoData {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm");

    private final ZoneId fuso = ZoneId.systemDefault();

    public String data(LocalDate data) {
        return data == null ? "" : DATA.format(data);
    }

    public String dia(OffsetDateTime momento) {
        return momento == null ? "" : DATA.format(momento.atZoneSameInstant(fuso));
    }

    public String dataHora(OffsetDateTime momento) {
        return momento == null ? "" : DATA_HORA.format(momento.atZoneSameInstant(fuso));
    }
}
