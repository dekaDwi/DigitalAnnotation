package com.deka.digitalannotation;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.TimingLogger;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.deka.digitalannotation.TouchImageViewNew.TouchImageView;
import com.deka.digitalannotation.matriks.Bezier;
import com.deka.digitalannotation.matriks.Label;
import com.deka.digitalannotation.matriks.Matriks;
import com.deka.digitalannotation.matriks.Spline;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MainActivity extends AppCompatActivity {

    final int RQS_IMAGE1 = 1;
    Button btnLoadImage, simpanMembran, simpanAnotasi, hapus;
    TextView textJudul, textLokasi, textResolusi, jumlahMembran/*, textHasil*/;
    TouchImageView imageResult;
    List<float[]> l = new ArrayList<float[]>();
    List<int[]> st = new ArrayList<int[]>();
    List<int[]> l2 = new ArrayList<int[]>();
    Drawable drawable;
    Rect imageBounds;
    KSArray Membran;
    Sel s;
    int x, y, cnt = 0, titikU, titikR;
    String lks;
    double scalH, scalW;
    boolean boleh = false;
    Uri source;
    Bitmap bitmapMaster, b, label, gambarAsli;
    Canvas canvasMaster, label2;
    Stack<Bitmap> undos = new Stack<Bitmap>();

    public static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnLoadImage = (Button) findViewById(R.id.btnPilihBerkas);
        simpanMembran = (Button) findViewById(R.id.btnSimpanMembran);
        simpanMembran.setEnabled(false);
        simpanAnotasi = (Button) findViewById(R.id.btnSimpan);
        simpanAnotasi.setEnabled(false);
        hapus = (Button) findViewById(R.id.btnHapus);
        hapus.setEnabled(false);
        textJudul = (TextView) findViewById(R.id.namaBerkas);
        textResolusi = (TextView) findViewById(R.id.resolusi);
        textLokasi = (TextView) findViewById(R.id.lokasiBerkas);
        jumlahMembran = (TextView) findViewById(R.id.infoJumlahMembran);
//        textHasil = (TextView) findViewById(R.id.hasilInput);
        imageResult = (TouchImageView) findViewById(R.id.gambar);
        s = new Sel();
        b = null;

        hapus.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputDialog();
            }
        });

        btnLoadImage.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, RQS_IMAGE1);
                imageResult.resetZoom();
                s = new Sel();
                boleh = false;
                l = new ArrayList<float[]>();
                simpanMembran.setEnabled(false);
                simpanAnotasi.setEnabled(false);
                hapus.setEnabled(false);
                undos.clear();
                Membran = new KSArray("Membran");
                jumlahMembran.setText("Number of object : " + Membran.count);
                cnt = 0;
            }
        });

        imageResult.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //boleh ==> jika sudah melakukan cek berkas
                if (boleh) {
                    int action = event.getAction();
                    x = (int) event.getX();
                    y = (int) event.getY();
                    PointF bitmapPoint = imageResult.transformCoordTouchToBitmap(event.getX(), event.getY(), true);
                    PointF normalizedBitmapPoint = new PointF(bitmapPoint.x / bitmapMaster.getWidth(), bitmapPoint.y / bitmapMaster.getHeight());
                    int xN = Math.round(normalizedBitmapPoint.x * bitmapMaster.getWidth());
                    int yN = Math.round(normalizedBitmapPoint.y * bitmapMaster.getHeight());
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            if (cnt == 0) {
                                //menyimpan bitmap awal, untuk semacam undo
                                st = new ArrayList<int[]>();
                                l = new ArrayList<float[]>();
                                b = Bitmap.createBitmap(bitmapMaster);
                                undos.push(b);
                                b = null;
                                s = new Sel();
                                cnt = 1;
                            } else {
                                //mengembalikan bitmap yang telah disimpan, jika ada
                                cnt = 0;
                                b = undos.pop();
                                canvasMaster.drawBitmap(b, 0, 0, null);
                                st = new ArrayList<int[]>();
                                l = new ArrayList<float[]>();
                                undos.push(b);
                                b = null;
                                s = new Sel();
                                imageResult.invalidate();
                            }
                            simpanMembran.setEnabled(false);
                            break;
                        case MotionEvent.ACTION_UP:
                            float[][] a = new float[l.size()][2];
                            for (int i = 0; i < l.size(); i++) {
                                for (int j = 0; j < 2; j++) {
                                    a[i][j] = l.get(i)[j];
                                }
                            }
                            closeSpline(a);

                            simpanMembran.setEnabled(true);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            //menandai sel/membran
                            float[] titik = {xN, yN};
                            l.add(titik);
                            cnt = 1;
                            point2(xN, yN, 1);
                            break;
                    }
                }

            /*
             * Return 'true' to indicate that the event have been consumed.
             * If auto-generated 'false', your code can detect ACTION_DOWN only,
             * cannot detect ACTION_MOVE and ACTION_UP.
             */
                return true;
            }
        });

        simpanMembran.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    int[][] koor = sort(l2);
                    label(koor, Membran.count+1);
                    TimingLogger timings = new TimingLogger("TopicLogTag", "simpan membran");
                    simpanAnotasi.setEnabled(false);
                    Membran.tambahData(s);
                    jumlahMembran.setText("Number of object : " + Membran.count);
                    canvasMaster.drawBitmap(undos.pop(), 0, 0, null);
                    imageResult.invalidate();
                    undos.clear();
                    cnt = 0;
                    Elemen temp = s.head;
                    if (!s.isEmpty()) {
                        point2(temp.x, temp.y, 2);
                        while (temp.next != null) {
                            temp = temp.next;
                            point2(temp.x, temp.y, 2);
                        }
                    }
                    gambarNo(s, Membran.count);
                    simpanAnotasi.setEnabled(true);
                    s = new Sel();
                    simpanMembran.setEnabled(false);
                    timings.addSplit("simpan membran");
                    timings.dumpToLog();
                } catch (ArrayIndexOutOfBoundsException e) {
                    Bitmap b = undos.pop();
                    canvasMaster.drawBitmap(b, 0, 0, null);
                    undos.push(b);
                    imageResult.invalidate();
//                    undos.clear();
                    toast("Terjadi kesalahan.\nUlangi menandai objek.");
                }
            }
        });

        simpanAnotasi.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                simpanFile();
                simpanLabel();
//                t();
            }
        });

    }

    public void showInputDialog() {
        LayoutInflater layoutInflater = LayoutInflater.from(MainActivity.this);
        View promptView = layoutInflater.inflate(R.layout.prompt, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilder.setView(promptView);

        final EditText editText = (EditText) promptView.findViewById(R.id.edittext);
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String hasil = editText.getText().toString();
                        int noObjek = Integer.parseInt(hasil);
                        if (noObjek >= 1 && noObjek <= Membran.count) {
                            hapusObjek(noObjek);
                        } else {
                            toast("Nomor objek salah");
                        }
                    }
                })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
    }

    public void hapusObjek(int idx) {
        Membran.hapusData(idx);
        bitmapMaster = Bitmap.createBitmap(gambarAsli.getWidth(), gambarAsli.getHeight(), gambarAsli.getConfig());
        canvasMaster = new Canvas(bitmapMaster);
        canvasMaster.drawBitmap(gambarAsli, 0, 0, null);
        imageResult.setImageBitmap(bitmapMaster);
        jumlahMembran.setText("Number of object : " + Membran.count);
        undos.clear();
        cnt = 0;
        gambarSel();
        hapusLabel(idx);
        simpanAnotasi.setEnabled(true);

    }

    public void hapusLabel(int idx) {
        for (int i = 0; i < label.getWidth(); i++) {
            for (int j = 0; j < label.getHeight(); j++) {
                int pixel = label.getPixel(i, j);
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);
                if (redValue == idx) {
                    point3(i, j, 0);
                }
            }
        }
        for (int i = 0; i < label.getWidth(); i++) {
            for (int j = 0; j < label.getHeight(); j++) {
                int pixel = label.getPixel(i, j);
                int redValue = Color.red(pixel);
                int greenValue = Color.green(pixel);
                int blueValue = Color.blue(pixel);
                int alphaValue = Color.alpha(pixel);
                if (redValue > idx && (redValue == greenValue && greenValue == blueValue)) {
                    int r = redValue - 1;
                    int g = greenValue - 1;
                    int b = blueValue - 1;
                    int warna = Color.argb(alphaValue, r, g, b);
                    label.setPixel(i, j, warna);
//                    if (r == g && g == b) {
//                        Paint paint = new Paint();
//                        paint.setStyle(Paint.Style.FILL);
//                        paint.setColor(Color.rgb(r, g, b));
//                        paint.setStrokeWidth(0);
//                        label2.drawCircle(i, j, 1, paint);
//                    }
                }
            }
        }
//        for (int i = idx; i <= Membran.count; i++) {
//
//        }
        toast("Objek berhasil dihapus");
    }

    public void toast(String teks) {
        Toast.makeText(this, teks, Toast.LENGTH_SHORT).show();
    }

    public void rapikan() {
        s = new Sel();
//        List<int[]> temp = new ArrayList<>();
        l2 = new ArrayList<int[]>();
        s.enQueue(st.get(0)[0], st.get(0)[1]);
        l2.add(st.get(0));
        titikR = 1;
        for (int i = 1; i < st.size(); i++) {
            if (st.get(i)[0] == st.get(i - 1)[0] && st.get(i)[1] == st.get(i - 1)[1]) {
            } else {
                s.enQueue(st.get(i)[0], st.get(i)[1]);
                l2.add(st.get(i));
                titikR++;
            }
        }
//        koorL = new int[titikR][2];
//        for (int i = 0; i < titikR; i++) {
//            koorL[i][0] = temp.get(i)[0];
//            koorL[i][1] = temp.get(i)[1];
//        }
    }

    public int[][] sort(List<int[]> koor) {
        TimingLogger timings = new TimingLogger("TopicLogTag", "sorting koordinat");
        int[][] hasil = new int[koor.size()][2];

        for (int i = 0; i < koor.size(); i++) {
            hasil[i] = koor.get(i);
        }

        for (int i = 1; i < koor.size(); i++) {
            for (int j = koor.size() - 1; j >= i; j--) {
                if (hasil[j - 1][1] > hasil[j][1]) {
                    int[] temp = hasil[j - 1];
                    hasil[j - 1] = hasil[j];
                    hasil[j] = temp;
                }
            }
        }
        timings.addSplit("sorting");
        timings.dumpToLog();
        return hasil;
    }

    public void label(int[][] koor, int obj) {
        TimingLogger timings = new TimingLogger("TopicLogTag", "labeling");
//        System.out.println("label");
        int[] d = Label.dimensi(koor);
        int[][] ah = Label.A(koor, d);
        Label.areaFill(ah, d);
//        int[][] ac = Label.AC(aa, d);
//        int[][] ah = Label.AH(aa, ac, d, obj);
//        System.out.println("gambar label");
        int pixel = Color.rgb(obj, obj, obj);
        for (int i = Label.awalBaris; i <= Label.akhirBaris; i++) {
            for (int j = Label.awalKolom; j <= Label.akhirKolom; j++) {
                if (ah[i - Label.awalBaris][j - Label.awalKolom] != 0) {
//                    point3(i, j, ah[i - Label.awalBaris][j - Label.awalKolom]);
//                    point3(i, j, obj);
                    label.setPixel(i, j, pixel);
                }
            }
        }
        Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show();

        timings.addSplit("labelling");
        timings.dumpToLog();
    }

//    public void label(int[][] koor, int obj) {
//        TimingLogger timings = new TimingLogger("TopicLogTag", "labeling");
////        int ymin = koor[0][1];
////        int ymax = koor[koor.length - 1][1];
//
////        for (int i = ymin; i <= ymax; i++) {
////            int xmax = 0;
////            for (int j = 0; j < koor.length; j++) {
////                if (koor[j][1] == i) {
////                    if (xmax < koor[j][0]) {
////                        xmax = koor[j][0];
////                    }
////                }
////            }
////            int xmin = xmax;
////            for (int j = 0; j < koor.length; j++) {
////                if (koor[j][1] == i) {
////                    if (xmin > koor[j][0]) {
////                        xmin = koor[j][0];
////                    }
////                }
////            }
////            boolean gambar = true;
////            for (int j = xmin; j <= xmax; j++) {
////                if (gambar) {
////                    point3(j, i, obj);
////                }
////
////
////            }
////        }
//
//
//
//        timings.addSplit("labelling");
//        timings.dumpToLog();
//    }

    public void closeSpline(float[][] a) {
        titikU = a.length;
        TimingLogger timings = new TimingLogger("TopicLogTag", "closeSpline");
        l2 = new ArrayList<int[]>();
        if (a.length <= 3) {
            return;
        }
        float[][] a2 = new float[a.length + 2][a[0].length];
        for (int i = 0; i < a.length; i++) {
            System.arraycopy(a[i], 0, a2[i], 0, 2); //1
        }
        System.arraycopy(a[1], 0, a2[a.length], 0, 2); //1
        System.arraycopy(a[2], 0, a2[a.length + 1], 0, 2); //1
        float[][] m1 = Matriks.invers(Spline.m1(a2));
        float[][] m2 = Spline.m2(a2);
        float[][] m3 = Matriks.kaliMatrix(m1, m2); //3
        float[][] m3m = new float[m3.length + 2][2]; //5
        System.arraycopy(a2[0], 0, m3m[0], 0, 2); //1
        for (int i = 1; i < m3m.length - 1; i++) { //2-4
            System.arraycopy(m3[i - 1], 0, m3m[i], 0, 2); //1
        }
        System.arraycopy(a2[a2.length - 1], 0, m3m[m3m.length - 1], 0, 2); //1

        for (int i = 1; i < a2.length - 2; i++) {
            float[] s1 = a2[i];
            float[] b1 = m3m[i];
            float[] b2 = m3m[i + 1];
            float[] s2 = a2[i + 1];
            float[][] t1 = Spline.tengah(s1, b1, b2, s2);
            float[][] t2 = Matriks.kaliMatrix(Matriks.kaliMatrix(t1, Bezier.basisBezier), Bezier.nilaiT());
            for (int b = 0; b < t2[0].length; b++) {
                int x = Math.round(t2[1][b]);
                int y = Math.round(t2[0][b]);
                point2(y, x, 3);
                int[] titik = {y, x};
                st.add(titik);
//                l2.add(titik);
//                s.enQueue(y, x);
            }
        }
        rapikan();
        timings.addSplit("Reconstruct the curve, titikU = " + titikU + " | titikR = " + titikR);
        timings.dumpToLog();
    }

    //membalikkan koordinat dari koordinat scale image menjadi koordinat asli
    final float[] getPointerCoords(ImageView view, MotionEvent e) {
        final int index = e.getActionIndex();
        final float[] coords = new float[]{e.getX(index), e.getY(index)};
        Matrix matrix = new Matrix();
        view.getImageMatrix().invert(matrix);
        matrix.postTranslate(view.getScrollX(), view.getScrollY());
        matrix.mapPoints(coords);
        return coords;
    }

    //membalikkan koordinat dari koordinat asli menjadi koordinat scale image
    final float[] coba(ImageView view, int x, int y) {
        double intrH = drawable.getIntrinsicHeight();
        double intrW = drawable.getIntrinsicWidth();
        scalW = imageResult.getWidth();
        scalH = imageResult.getHeight();
        double ratioH = scalH / intrH;
        double ratioW = scalW / intrW;
        x = (int) (x * ratioW);
        y = (int) (y * ratioH);
        final float[] coords = new float[]{x, y};
        return coords;
    }

    //penyimpanan file
    public void simpanFile() {
        TimingLogger timings = new TimingLogger("TopicLogTag", "simpanFile");
        String namaFile = textJudul.getText().toString();
        String lokasiFile = textLokasi.getText().toString();
        int name = namaFile.lastIndexOf(".");
        String nama = namaFile.substring(0, name) + ".txt";
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, nama);
            Writer wr = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF8"));
            wr.append("Anotasi dari file " + namaFile + "\n");
            wr.append("Lokasi:" + lokasiFile + "\n");
            wr.append(Membran.isi() + "\n");
            wr.flush();
            wr.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        timings.addSplit("simpan file");
        timings.dumpToLog();
    }

    public void simpanLabel() {
        String namaFile = textJudul.getText().toString();
        int name = namaFile.lastIndexOf(".");
        String nama = namaFile.substring(0, name) + ".png";
        try {
            File root = new File(Environment.getExternalStorageDirectory(), "Notes");
            if (!root.exists()) {
                root.mkdirs();
            }
            File file = new File(root, nama);
            label.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show();
    }

    //pembacaan file anotasi dari file yang sama jika ada
    public void readFile() throws FileNotFoundException {
        TimingLogger timings = new TimingLogger("TopicLogTag", "membaca file");
        String namaFile = textJudul.getText().toString();
        int name = namaFile.lastIndexOf(".");
        String nama = namaFile.substring(0, name) + ".txt";
        String nama2 = namaFile.substring(0, name) + ".png";
        File sdCard = new File(Environment.getExternalStorageDirectory(), "Notes");
        File file = new File(sdCard, nama);
        Sel s;
        if (file.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(file));
                String line;
                int c = 1;
                while ((line = br.readLine()) != null) {
                    if (c == 1) {
                        c++;
                    } else {
                        if (line.indexOf("/") == -1 || line.indexOf("L") != -1) {
                            Membran = new KSArray(line);
                        } else if (line.indexOf("/") != -1) {
                            s = new Sel();
                            String angka = line;
                            int awal = angka.indexOf("(");
                            int x = Integer.parseInt(angka.substring(awal + 1, angka.indexOf(",", awal)));
                            int y = Integer.parseInt(angka.substring(angka.indexOf(",", awal) + 2, angka.indexOf(")", awal + 1)));
                            s.enQueue(x, y);
                            while (angka.indexOf("(", awal + 1) != -1) {
                                awal = angka.indexOf("(", awal + 1);
                                x = Integer.parseInt(angka.substring(awal + 1, angka.indexOf(",", awal)));
                                y = Integer.parseInt(angka.substring(angka.indexOf(",", awal) + 2, angka.indexOf(")", awal + 1)));
                                s.enQueue(x, y);
                            }
                            Membran.tambahData(s);
                        } else if (line.substring(0, 5).equals("Belum")) {
                        }
                    }
                }
                br.close();
                jumlahMembran.setText("Number of object : " + Membran.count);
                gambarSel();
            } catch (IOException e) {
            }
        } else {
            Toast.makeText(this, "This image has not been annotated.", Toast.LENGTH_SHORT).show();
            simpanAnotasi.setEnabled(false);
            Membran = new KSArray("Membran");
            jumlahMembran.setText("Number of object : " + Membran.count);
        }
        timings.addSplit("Membaca file");
        timings.dumpToLog();

    }

    //bagian dari pembacaan file, untuk menggambar yang telah dianotasi
    public void gambarSel() {
        Elemen e;
//        s = Membran.head;
        int idx = 1;
        for (Sel s : Membran.getAll()) {
            if (s != null) {
                e = s.head;
                if (e != null) {
                    point2(e.x, e.y, 2);
                    while (e.next != null) {
                        e = e.next;
                        point2(e.x, e.y, 2);
                    }
                }
            }
            gambarNo(s, idx);
            idx++;
        }
    }

    public void gambarNo(Sel s, int idx) {
        Elemen e;
        int xMax = 0;
        int yMax = 0;
        if (s != null) {
            e = s.head;
            if (e != null) {
                if (e.x > xMax) {
                    xMax = e.x;
                }
                if (e.y > yMax) {
                    yMax = e.y;
                }
                while (e.next != null) {
                    e = e.next;
                    if (e.x > xMax) {
                        xMax = e.x;
                    }
                    if (e.y > yMax) {
                        yMax = e.y;
                    }
                }
            }
        }
        int xMin = xMax;
        int yMin = yMax;
        if (s != null) {
            e = s.head;
            if (e != null) {
                if (e.x < xMin) {
                    xMin = e.x;
                }
                if (e.y < yMin) {
                    yMin = e.y;
                }
                while (e.next != null) {
                    e = e.next;
                    if (e.x < xMin) {
                        xMin = e.x;
                    }
                    if (e.y < yMin) {
                        yMin = e.y;
                    }
                }
            }
        }
        int x = ((xMax + xMin) / 2);
        int y = ((yMax + yMin) / 2);
        String teks = "" + idx;
        Paint paint = new Paint();
        paint.setColor(Color.YELLOW);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextSize(30);
        canvasMaster.drawText(teks, x, y, paint);
        imageResult.invalidate();
    }

    //untuk menggambar titik sesuai dengan masukkan touch dari user

    public void point(int x, int y, int kondisi) {
        if (x < 0 || y < 0 || x > imageResult.getWidth() || y > imageResult.getHeight()) {
            return;
        } else {
            int projectedX = (int) ((double) x * ((double) bitmapMaster.getWidth() / (double) imageResult.getWidth()));
            int projectedY = (int) ((double) y * ((double) bitmapMaster.getHeight() / (double) imageResult.getHeight()));
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            switch (kondisi) {
                case 1:
                    paint.setColor(Color.CYAN);
                    break;
                case 2:
                    paint.setColor(Color.RED);
                    break;
                case 3:
                    paint.setColor(Color.GREEN);
                    break;
            }
            paint.setStrokeWidth(3);
            canvasMaster.drawCircle(projectedX, projectedY, 3, paint);
            imageResult.invalidate();
        }
    }

    public void point2(int x, int y, int kondisi) {
        if (x < 0 || y < 0 || x > bitmapMaster.getWidth() || y > bitmapMaster.getHeight()) {
            return;
        } else {
            int projectedX = x;
            int projectedY = y;
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            switch (kondisi) {
                case 1:
                    paint.setColor(Color.YELLOW);
                    break;
                case 2:
                    paint.setARGB(255, 255, 0, 0);
                    break;
                case 3:
                    paint.setColor(Color.BLUE);
                    break;
            }
            paint.setStrokeWidth(3);
            canvasMaster.drawCircle(projectedX, projectedY, 3, paint);
            imageResult.invalidate();
        }
    }

    public void point3(int x, int y, int obj) {
        if (x < 0 || y < 0 || x > bitmapMaster.getWidth() || y > bitmapMaster.getHeight()) {
            return;
        } else {
            int projectedX = x;
            int projectedY = y;
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.rgb(obj, obj, obj));
            paint.setStrokeWidth(0);
            label2.drawCircle(projectedX, projectedY, 1, paint);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        TimingLogger timings = new TimingLogger("TopicLogTag", "membukaGambar");
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap tempBitmap;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case RQS_IMAGE1:
                    source = data.getData();
                    int a = source.getPath().lastIndexOf("/");
                    lks = source.getPath().substring(0, a);
                    String lokasi = ImageFilePath.getPath(getApplicationContext(), source);
                    int name = lokasi.lastIndexOf("/");
                    String nama = lokasi.substring(name + 1);
                    int b = lokasi.lastIndexOf("/");
                    lokasi = lokasi.substring(0, b + 1);
                    textLokasi.setText(lokasi);
                    textJudul.setText(nama);
                    try {
                        tempBitmap = BitmapFactory.decodeStream(
                                getContentResolver().openInputStream(source));
                        Bitmap.Config config;
                        if (tempBitmap.getConfig() != null) {
                            config = tempBitmap.getConfig();
                        } else {
                            config = Bitmap.Config.ARGB_8888;
                        }
                        bitmapMaster = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), config);
                        gambarAsli = Bitmap.createBitmap(tempBitmap);
                        undos.add(bitmapMaster);
                        canvasMaster = new Canvas(bitmapMaster);
                        canvasMaster.drawBitmap(tempBitmap, 0, 0, null);
                        imageResult.setImageBitmap(bitmapMaster);
                        drawable = imageResult.getDrawable();
                        imageBounds = drawable.getBounds();
                        int x = bitmapMaster.getWidth();
                        int y = bitmapMaster.getHeight();
                        textResolusi.setText(x + " x " + y);
                        String namaFile = nama;
                        int titik = namaFile.lastIndexOf(".");
                        String nama2 = namaFile.substring(0, titik) + ".png";
                        Bitmap tempBitmap2;
                        File sdCard = new File(Environment.getExternalStorageDirectory(), "Notes");
                        File file = new File(sdCard, nama2);
                        if (file.exists()) {
                            Uri aa = getImageContentUri(getApplicationContext(), file);
                            try {
                                tempBitmap2 = BitmapFactory.decodeStream(getContentResolver().openInputStream(aa));
                                Bitmap.Config config2;
                                if (tempBitmap2.getConfig() != null) {
                                    config2 = tempBitmap2.getConfig();
                                } else {
                                    config2 = Bitmap.Config.ARGB_8888;
                                }
                                label = Bitmap.createBitmap(tempBitmap2.getWidth(), tempBitmap2.getHeight(), config2);
                                label2 = new Canvas(label);
                                label2.drawBitmap(tempBitmap2, 0, 0, null);
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }
                        } else {
                            label = Bitmap.createBitmap(tempBitmap.getWidth(), tempBitmap.getHeight(), config);
                            label2 = new Canvas(label);
                            label2.drawBitmap(label, 0, 0, null);
                        }

                        timings.addSplit("Membuka gambar");
                        timings.dumpToLog();
                        readFile();
                        boleh = true;
                        hapus.setEnabled(true);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                    break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
