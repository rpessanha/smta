package mei.tcd.util;

import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Created by pessanha on 03-08-2013.
 */
public class SensorWriterSmta {
    // Representa caminhos, existentes ou não
    public File file;
    // Instancia classe FileWriter para escrever ficheiro
    private FileWriter fileWriter;

    /**
     * Criar ficheiro
     *
     * @param dir Diretório relativo para guardar dados
     * @param nome Nome do ficheiro
     */
    public void createFile(String subDir, String dir, String nome)
    {

        // Criar a pasta usando o objecto File
        File directory = new File(Environment.getExternalStorageDirectory() + "/"+dir+"/" );
        // Verifica se ela existe, se não existir então cria
        if (!directory.exists())
        {
            Log.v("SMTA Create File", "A criar directorio.");
            directory.mkdir();
        }
        // Criar a subpasta usando o objecto File
        File subdirectory = new File(Environment.getExternalStorageDirectory() + "/"+dir+"/" + "/"+subDir+"/" );
        // Verifica se ela existe, se não existir então cria
        if (!subdirectory.exists())
        {
            Log.v("SMTA Create File", "A criar subdirectorio.");
            subdirectory.mkdir();
        }
        // Classe Data. vai até ao milisegundo. Java.util
        Date data = new Date();
        // Criação do nome do ficheiro com o objeto FILE que va ser gravado no sdcard. Formatação <ano_mês_dia_hora_minuto_segundo>
        String fileName = android.text.format.DateFormat.format("yyyy_MM_dd_hh_mm_ss", data).toString();
        file = new File(Environment.getExternalStorageDirectory() + "/"+dir+"/" + "/"+subDir+"/" + nome+fileName + ".csv");
        try {
            fileWriter = new FileWriter(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            Log.v("SMTA Create File Error: ", e.getStackTrace().toString());
        }
        // Tenho de colocar a permiss?o para escrever no storage caso contr?rio gera erro.
    }

    /**
     * Escreve strings para o ficheiro se existir
     *
     * @param dados String de dados para serem escritos.
     */
    public void writeThis(String dados){
        try {
            if (!file.exists())
            {
                Log.v("SMTA Write This: ", "Não existe ficheiro, vou criá-lo.");

                file.createNewFile();
            }
            fileWriter.write(dados);
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("SMTA writeThis Error: ", e.getStackTrace().toString());
        }
    }

    /**
     * Fecha ficheiro
     *
     */
    public void closeFile()
    {
        try {
             fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.v("SMTA closeFile Error: ", e.getStackTrace().toString());
        }

    }
}
