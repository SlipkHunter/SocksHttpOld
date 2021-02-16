package com.slipkprojects.ultrasshservice.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

public class StreamGobbler extends Thread {
   private final BufferedReader reader;
   private List<String> writer;
   private StreamGobbler.OnLineListener listener;

   public StreamGobbler(InputStream inputStream, List<String> outputList) {
      this.reader = new BufferedReader(new InputStreamReader(inputStream));
      this.writer = outputList;
   }

   public StreamGobbler(InputStream inputStream, StreamGobbler.OnLineListener onLineListener) {
      this.reader = new BufferedReader(new InputStreamReader(inputStream));
      this.listener = onLineListener;
   }

   public void run() {
      while(true) {
         try {
            String line;
			
            if ((line = this.reader.readLine()) != null) {
               if (this.writer != null) {
                  this.writer.add(line);
               }

               if (this.listener != null) {
                  this.listener.onLine(line);
               }
			   
               continue;
            }
			
         } catch (IOException var3) {}

         try {
            this.reader.close();
         } catch (IOException var2) {}

         return;
      }
   }

   public interface OnLineListener {
      void onLine(String var1);
   }
}
