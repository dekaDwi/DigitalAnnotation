package com.deka.digitalannotation.matriks;

import java.util.Stack;

/**
 * Created by demeg on 04-Nov-15.
 */
public class Label {

    public static int awalBaris, akhirBaris, awalKolom, akhirKolom;

    public static int[] dimensi(int[][] koor) {
        int[] hasil = new int[2];
        int a, b, kolom, baris, size = koor.length - 1;

        //kolom
        a = koor[0][1];
        awalKolom = a;
        b = koor[size][1];
        akhirKolom = b;
        kolom = (b - a) + 1;

        //baris
        b = 0;
        for (int i = 0; i < size + 1; i++) {
            if (koor[i][0] > b) {
                b = koor[i][0];
            }
        }
        akhirBaris = b;
        a = b;
        for (int i = 0; i < size + 1; i++) {
            if (koor[i][0] < a) {
                a = koor[i][0];
            }
        }
        awalBaris = a;
        baris = (b - a) + 1;

        //gabung
        hasil[0] = baris;
        hasil[1] = kolom;

        return hasil;
    }

    public static int[][] A(int[][] koor, int[] dimensi) {
        int baris = dimensi[0];
        int kolom = dimensi[1];
        int[][] hasil = new int[baris][kolom];
        for (int[] koor1 : koor) {
            hasil[koor1[0] - awalBaris][koor1[1] - awalKolom] = 1;
        }
        return hasil;
    }

    public static int[][] AC(int[][] A, int[] dimensi) {
        int baris = dimensi[0];
        int kolom = dimensi[1];
        int[][] hasil = new int[baris][kolom];
        for (int i = 0; i < baris; i++) {
            for (int j = 0; j < kolom; j++) {
                if (A[i][j] == 0) {
                    hasil[i][j] = 1;
                } else {
                    hasil[i][j] = 0;
                }
            }
        }
        return hasil;
    }

    public static int[][] AH(int[][] A, int[][] AC, int[] dimensi, int label) {
        int baris = dimensi[0];
        int kolom = dimensi[1];
        int[][] hasil = new int[baris][kolom];

        //cari titik awal
        boolean ketemu = false;
        int b = 0;
        int k = 0;
        int ii = 1;
        while (ii < baris && !ketemu) {
            int j = 1;
            while (j < kolom && !ketemu) {
                if (A[ii][j] == 0 && (A[ii][j - 1] == 1 && A[ii - 1][j] == 1)) {
                    b = ii;
                    k = j;
                    ketemu = true;
                }
                j++;
            }
            ii++;
        }

        //proses filling awal
        hasil[b][k] = 1;
        int temp = 1;
        for (int i = 1; i <= baris - 2; i++) {
            int k1 = kolom-2;
            for (int j = 1; j <= kolom - 2; j++) {
                if (hasil[i][j] == 1) {
                    hasil[i - 1][j] = (AC[i - 1][j] == 1 && temp == 1) ? 1 : 0;
                    hasil[i + 1][j] = (AC[i + 1][j] == 1 && temp == 1) ? 1 : 0;
                    hasil[i][j - 1] = (AC[i][j - 1] == 1 && temp == 1) ? 1 : 0;
                    hasil[i][j + 1] = (AC[i][j + 1] == 1 && temp == 1) ? 1 : 0;
                }
                if (hasil[i][k1] == 1) {
                    hasil[i - 1][k1] = (AC[i - 1][k1] == 1 && temp == 1) ? 1 : 0;
                    hasil[i + 1][k1] = (AC[i + 1][k1] == 1 && temp == 1) ? 1 : 0;
                    hasil[i][k1 - 1] = (AC[i][k1 - 1] == 1 && temp == 1) ? 1 : 0;
                    hasil[i][k1 + 1] = (AC[i][k1 + 1] == 1 && temp == 1) ? 1 : 0;
                }
                k1--;
            }
        }
        for (int j = 1; j <= kolom - 2; j++) {
            int b1 = baris-2;
            for (int i = 1; i <= baris - 2; i++) {
                if (hasil[i][j] == 1) {
                    hasil[i - 1][j] = (AC[i - 1][j] == 1 && temp == 1) ? 1 : 0;
                    hasil[i + 1][j] = (AC[i + 1][j] == 1 && temp == 1) ? 1 : 0;
                    hasil[i][j - 1] = (AC[i][j - 1] == 1 && temp == 1) ? 1 : 0;
                    hasil[i][j + 1] = (AC[i][j + 1] == 1 && temp == 1) ? 1 : 0;
                }
                if (hasil[b1][j] == 1) {
                    hasil[b1 - 1][j] = (AC[b1 - 1][j] == 1 && temp == 1) ? 1 : 0;
                    hasil[b1 + 1][j] = (AC[b1 + 1][j] == 1 && temp == 1) ? 1 : 0;
                    hasil[b1][j - 1] = (AC[b1][j - 1] == 1 && temp == 1) ? 1 : 0;
                    hasil[b1][j + 1] = (AC[b1][j + 1] == 1 && temp == 1) ? 1 : 0;
                }
                b1--;
            }
        }
        //penggabungan
        for (int i = 0; i < baris; i++) {
            for (int j = 0; j < kolom; j++) {
                if (A[i][j] == 1 || hasil[i][j] == 1) {
                    hasil[i][j] = label;
                }
            }
        }

        return hasil;
    }

    public static void areaFill(int[][] ah, int[] dimensi) {
        int baris = dimensi[0];
        int kolom = dimensi[1];

        //mencari titik awal
        boolean ketemu = false;
        int b = 0;
        int k = 0;
        int ii = 1;
        while (ii < baris && !ketemu) {
            int j = 1;
            while (j < baris && !ketemu) {
                if (ah[ii][j] == 0 && (ah[ii][j - 1] == 1 && ah[ii - 1][j] == 1)) {
                    b = ii;
                    k = j;
                    ketemu = true;
                }
                j++;
            }
            ii++;
        }

        Stack<int[]> st = new Stack<>();
        st.push(new int[]{b, k});
        boolean selesai = false;
        while (!selesai) {
            int[] titik = st.pop();
            int i = titik[0];
            int j = titik[1];
            while (ah[i][j] == 0) {
                ah[i][j] = 1;
                j++;
            }
            //cari seed baru di bawah
            int jTemp = j - 1;
            j = jTemp;
            i = i + 1;
            while (j >= titik[1]) {
                boolean mundur = false;
                while (ah[i][j] == 0) {
                    j--;
                    mundur = true;
                }
                if (!mundur) {
                    j--;
                }
                st.push(new int[]{i, j + 1});
            }
            //cari seed baru di atas
            j = jTemp;
            i = i - 2;
            while (j >= titik[1]) {
                boolean mundur = false;
                while (ah[i][j] == 0) {
                    j--;
                    mundur = true;
                }
                if (!mundur) {
                    j--;
                }
                st.push(new int[]{i, j + 1});
            }
            if (st.isEmpty()) {
                selesai = true;
            }
        }
    }

}
