package com.ukdw.rplbo.soal_ug_javafx;

import com.ukdw.rplbo.soal_ug_javafx.data.Mahasiswa_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Matakuliah_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Nilai_table;
import com.ukdw.rplbo.soal_ug_javafx.entity.Mahasiswa;
import com.ukdw.rplbo.soal_ug_javafx.entity.Matakuliah;
import com.ukdw.rplbo.soal_ug_javafx.entity.Nilai;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AppController {
    @FXML
    private ComboBox<String> option;
    @FXML
    private TableView<Object> table;
    @FXML
    private TableColumn<Object,String> column1;
    @FXML
    private TableColumn<Object,String> column2;
    @FXML
    private TableColumn<Object,String> column3;

    @FXML
    private BarChart<String, Number> barchart;
    @FXML
    private LineChart<String, Number> linechart;
    @FXML
    private PieChart piechart;


    Mahasiswa_table mhs_table = new Mahasiswa_table();
    Matakuliah_table mtkl_table = new Matakuliah_table();
    Nilai_table nilai_table = new Nilai_table();


    public AppController() throws SQLException {
    }

    @FXML
    public void initialize() throws SQLException {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Mahasiswa",
                "Matakuliah"
        );
        option.setItems(options);
        option.setValue("Mahasiswa");

        option.valueProperty().addListener((observable, oldValue, newValue) -> {
            table.getItems().clear();

            if ("Matakuliah".equals(newValue)) {
                linechart.setVisible(true);
                column1.setText("kode_mk");
                column1.setCellValueFactory(new PropertyValueFactory<>("kode_mk"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));


                column3.setText("sks");
                column3.setCellValueFactory(new PropertyValueFactory<>("sks"));

                table.setItems(FXCollections.observableArrayList(mtkl_table.fetch_all_matkul()));
            } else {
                linechart.setVisible(false);
                column1.setText("NIM");
                column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));

                column3.setText(" ");
                column3.setCellValueFactory(null);

                table.setItems(FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa()));
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {

                if (newSelection instanceof Mahasiswa) {
                    Mahasiswa m = (Mahasiswa) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getNIM() + ")");

                    // -- chart --
                    update_barchart("nim",m.getNIM());
                    update_piechart("nim",m.getNIM());


                } else if (newSelection instanceof Matakuliah) {
                    Matakuliah m = (Matakuliah) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getKode_mk() + ")");

                    // -- chart --
                    update_barchart("kode_mk",m.getKode_mk());
                    update_piechart("kode_mk",m.getKode_mk());
                    update_linechart(m.getKode_mk());
                }
            }
        });

        linechart.setVisible(false);
        column1.setText("NIM");
        column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
        column2.setText("nama");
        column2.setCellValueFactory(new PropertyValueFactory<>("nama"));
        column3.setText(" ");

        ObservableList<Object> data = FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa());
        table.setItems(data);

    }

    public void update_barchart(String target_col, String val) {
        // 1. Bersihkan data lama
        barchart.getData().clear();
        barchart.setAnimated(false); // Mematikan animasi agar update lebih stabil

        // 2. Siapkan Series baru
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Jumlah Nilai (" + val + ")");

        // 3. Ambil data dari database berdasarkan target_col (nim atau kode_mk)
        List<Nilai> daftarNilai = nilai_table.fetch_nilai_by(target_col, val);

        // 4. Hitung frekuensi setiap grade (A, A-, B+, dst)
        // Gunakan array penilaian yang sudah ada di Nilai_table untuk urutan yang konsisten
        for (String grade : nilai_table.penilaian) {
            long count = 0;
            for (Nilai n : daftarNilai) {
                if (n.getNilai().equals(grade)) {
                    count++;
                }
            }
            series.getData().add(new XYChart.Data<>(grade, count));
        }

        // 5. Masukkan ke barchart
        barchart.getData().add(series);
    }

    public void update_linechart(String kode_mk) {
        // 1. Bersihkan data lama
        linechart.getData().clear();
        linechart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rata-rata Nilai per Angkatan");

        // 2. Ambil semua nilai untuk mata kuliah ini
        List<Nilai> daftarNilai = nilai_table.fetch_nilai_by_kode_mk(kode_mk);

        // 3. Kelompokkan nilai berdasarkan angkatan mahasiswa
        // Key: Angkatan (Integer), Value: List of scores (Double)
        java.util.Map<Integer, List<Double>> mapAngkatan = new java.util.TreeMap<>();

        for (Nilai n : daftarNilai) {
            try {
                // Ambil data mahasiswa untuk mendapatkan angkatannya
                Mahasiswa m = mhs_table.fetch_mahasiswa_by_nim(n.getNIM());
                if (m != null) {
                    int angkatan = m.getAngkatan();
                    mapAngkatan.putIfAbsent(angkatan, new ArrayList<>());
                    mapAngkatan.get(angkatan).add(n.get_converted_nilai());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 4. Hitung Mean (rata-rata) untuk setiap angkatan dan masukkan ke chart
        for (java.util.Map.Entry<Integer, List<Double>> entry : mapAngkatan.entrySet()) {
            List<Double> scores = entry.getValue();
            double sum = 0;
            for (Double s : scores) sum += s;
            double mean = sum / scores.size();

            series.getData().add(new XYChart.Data<>(entry.getKey().toString(), mean));
        }

        linechart.getData().add(series);
    }

    public void update_piechart(String target_col, String val) {
        // 1. Bersihkan data lama
        piechart.getData().clear();
        piechart.setAnimated(false);

        // 2. Ambil data dari database
        List<Nilai> daftarNilai = nilai_table.fetch_nilai_by(target_col, val);

        // 3. Hitung jumlah tiap grade dan masukkan jika jumlahnya > 0
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        for (String grade : nilai_table.penilaian) {
            int count = 0;
            for (Nilai n : daftarNilai) {
                if (n.getNilai().equals(grade)) {
                    count++;
                }
            }

            if (count > 0) {
                pieData.add(new PieChart.Data(grade + " (" + count + ")", count));
            }
        }

        // 4. Set data ke piechart
        piechart.setData(pieData);
    }
}
