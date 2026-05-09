package model;

public class ProdukData {
    private String nama;
    private String kode;
    private String kategori;
    private String satuan;
    private String deskripsi;
    private String foto;

    public ProdukData(String nama, String kode, String kategori,
                      String satuan, String deskripsi, String foto) {
        this.nama = nama;
        this.kode = kode;
        this.kategori = kategori;
        this.satuan = satuan;
        this.deskripsi = deskripsi;
        this.foto = foto;
    }

    public String getNama() { return nama; }
    public String getKode() { return kode; }
    public String getKategori() { return kategori; }
    public String getSatuan() { return satuan; }
    public String getDeskripsi() { return deskripsi; }
    public String getFoto() { return foto; }
}