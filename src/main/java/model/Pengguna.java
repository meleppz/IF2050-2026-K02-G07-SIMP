package model;

public class Pengguna {
    private String nik;
    private String nama;
    private String username;
    private String password;
    private String nomorTelepon;
    private String email;
    private Peran peran;
    private String divisi;
    private boolean statusAktif;

    public Pengguna(String nik, String nama, String username, String password, Peran peran) {
        this.nik = nik;
        this.nama = nama;
        this.username = username;
        this.password = password;
        this.peran = peran;
        this.statusAktif = true;
    }

    // Getters
    public String getNik()           { return nik; }
    public String getNama()          { return nama; }
    public String getUsername()      { return username; }
    public String getPassword()      { return password; }
    public String getNomorTelepon()  { return nomorTelepon; }
    public String getEmail()         { return email; }
    public Peran getPeran()          { return peran; }
    public String getDivisi()        { return divisi; }
    public boolean isStatusAktif()   { return statusAktif; }

    // Setters
    public void setNomorTelepon(String v) { this.nomorTelepon = v; }
    public void setEmail(String v)        { this.email = v; }
    public void setDivisi(String v)       { this.divisi = v; }
    public void setStatusAktif(boolean v) { this.statusAktif = v; }
}