package util;

import model.Pengguna;
import model.Peran;

public class Session {
    private static Session instance;
    private Pengguna penggunaAktif;

    private Session() {}

    public static Session getInstance() {
        if (instance == null) {
            instance = new Session();
        }
        return instance;
    }

    public void login(Pengguna pengguna) {
        this.penggunaAktif = pengguna;
    }

    public void logout() {
        this.penggunaAktif = null;
    }

    public Pengguna getPenggunaAktif() {
        return penggunaAktif;
    }

    public boolean isLoggedIn() {
        return penggunaAktif != null;
    }

    public String getNikAktif() {
        return penggunaAktif != null ? penggunaAktif.getNik() : null;
    }

    // ✅ Role-based access control
    public boolean isOperator() {
        return penggunaAktif != null && penggunaAktif.getPeran() == Peran.OPERATOR;
    }

    public boolean isSupervisor() {
        return penggunaAktif != null && penggunaAktif.getPeran() == Peran.SUPERVISOR;
    }
}