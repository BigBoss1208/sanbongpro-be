package com.example.becicd.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class TuChoiDatSanRequest {

    @NotBlank(message = "Vui lòng nhập lý do từ chối")
    @Size(max = 500, message = "Lý do từ chối tối đa 500 ký tự")
    private String lyDo;

    public String getLyDo() { return lyDo; }
    public void setLyDo(String lyDo) { this.lyDo = lyDo; }
}
