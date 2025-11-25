package com.confApi.db.enumeradores;

public enum EnumSexoHospedeHotel {
    Masculino(1, "Male"),
    Feminino(0, "Female");

    private final int idSexo;
    private final String descSexo;

    private EnumSexoHospedeHotel(int idSexo, String descSexo) {
        this.idSexo = idSexo;
        this.descSexo = descSexo;
    }

    public int getIdSexo() {
        return idSexo;
    }

    public String getDescSexo() {
        return descSexo;
    }



}
