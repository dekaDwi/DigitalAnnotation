package com.deka.digitalannotation;

import java.util.ArrayList;

/**
 * Created by demeg on 29/11/2015.
 */
public class KSArray {

    ArrayList<Sel> sel;
    String name;
    int count;

    public KSArray(String name) {
        this.name = name;
        sel = new ArrayList<>();
        count=0;
    }

    public boolean isEmpty() {
        return sel.isEmpty();
    }

    public void tambahData(Sel s) {
        sel.add(s);
        count++;
    }

    public ArrayList<Sel> getAll() {
        return sel;
    }

    public void printAll() {
        if (isEmpty()) {
            System.out.println("Kosoong!!!");
        } else {
            int idx = 1;
            for (Sel s : getAll()) {
                s.addName(name + "/" + idx, "tanggal");
                System.out.println(s);
                idx++;
            }
        }
    }

    public Sel aksesData(int idx) {
        idx = idx - 1;
        sel.get(idx).addName(name + "/" + (idx + 1), "tanggal");
        return sel.get(idx);
    }

    public void hapusData(int idx) {
        idx = idx - 1;
        if (idx < 0 || idx >= sel.size()) {
            System.out.println("idx error");
        } else {
            sel.remove(idx);
            count--;
        }
    }

    public String isi() {
        String isi = name;
        if (isEmpty()) {
            isi = isi + "\nBelum ada sel yang tersimpan";
        } else {
            int idx = 1;
            for (Sel sTemp : getAll()) {
                sTemp.addName(name + "/" + idx, "tanggal");
                isi = isi + "\n" + sTemp;
                idx++;
            }
        }
        return isi;
    }
}
