package fr.formiko.usual;

import fr.formiko.usual.read;
import fr.formiko.usual.structures.listes.GString;
import fr.formiko.usual.structures.listes.Liste;
import fr.formiko.usual.types.str;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
*{@summary tools about Files.}
*@author Hydrolien Baeldung
*@lastEditedVersion 2.28
*/
public class fichier {
  private static Progression progression;

  // CONSTRUCTORS ----------------------------------------------------------------

  // GET SET ----------------------------------------------------------------------
  public static Progression getProgression() {return progression;}
	public static void setProgression(Progression progression) {fichier.progression=progression;}
  // FUNCTIONS -----------------------------------------------------------------
  /**
  *make a liste of all .java file in the directory f.
  *@param f The directory were to search java file.
  *@lastEditedVersion 1.13
  */
  public static GString listerLesFichiersDuRep(File f) {
    GString gs = new GString();
    //parcourir les dossiers puis les sous dossiers etc jusqu'a ce que tout les fichiers soit traité,
    //cad sous la forme rep+sousdossier1+sousdossier2+nomDu.java
    if (f.isDirectory()){
      File allF [] = f.listFiles();
      if (allF != null) {
        for (File file : allF) {
          gs.add(listerLesFichiersDuRep(file));
        }
      }
    }else if(f.isFile()){
      gs.add(f.getPath());
    }
    return gs;
  }public static GString listerLesFichiersDuRep(String rep){return listerLesFichiersDuRep(new File(rep));}


  /**
   *{@summary Delete a directory and all his content.}<br>
   *If it's a folder it will call deleteDirectory on all sub file/folder and then destroy itself.
   *If it's a file it will destroy itself.
   *@lastEditedVersion 2.28
   */
  public static boolean deleteDirectory(File directoryToBeDeleted) {
    if(directoryToBeDeleted==null || !directoryToBeDeleted.exists()){return false;}
    File allF [] = directoryToBeDeleted.listFiles();
    if(allF!=null){
      for (File file : allF) {
        deleteDirectory(file);
      }
    }
    return directoryToBeDeleted.delete();
  }public static boolean deleteDirectory(String s){return deleteDirectory(new File(str.sToDirectoryName(s)));}

  public static void affichageDesLecteurALaRacine (File f) {
    erreur.println("Affichage des lecteurs à la racine du PC : ");
    for(File file : f.listRoots()){
      erreur.println(file.getAbsolutePath());
      try {
        int i = 1;
        //On parcourt la liste des fichiers et répertoires
        for(File nom : file.listFiles()){
          //S'il s'agit d'un dossier, on ajoute un "/"
          erreur.print("\t\t" + ((nom.isDirectory()) ? nom.getName()+"/" : nom.getName()));

          if((i%4) == 0){
            erreur.print("\n");
          }
          i++;
        }
        erreur.println("\n");
      } catch (NullPointerException e) {} //can be throw if there is no file.
    }
  }
  public static void fichierCiblePeuAvoirCeNom(String nom) {
    File f = new File(nom);
    //if (f.exists()) { throw new FichierDejaPresentException (nom);}
  }
  public static void copierUnFichier(String fileSourceName){
    String fileTargetName = read.getString("Nom du nouveau fichier","Copie de " + fileSourceName);
    copierUnFichier(fileSourceName, fileTargetName);
  }
  public static void copierUnFichier(String fileSourceName, String fileTargetName){
    Path source = Paths.get(fileSourceName);
    Path target = Paths.get(fileTargetName);
    try {
      Files.copy(source, target);
    }catch (IOException e) {
      erreur.erreur("fail to copy file from "+fileSourceName+" to "+fileTargetName);
    }
  }
  /**
  *{@summary Download a file from the web.}<br>
  *@param urlPath the url as a String
  *@param fileName the name of the file were to save data from the web
  *@param withInfo if true launch a thread to have info during download
  *@lastEditedVersion 2.7
  */
  public static boolean download2(String urlPath, String fileName, boolean withInfo) throws Exception {
    // String reason=null;
    Exception ex=null;
    DownloadThread downloadThread=null;
    FileOutputStream fos=null;
    try {
      URL url = new URL(urlPath);
      long fileToDowloadSize = getFileSize(url);
      ReadableByteChannel readChannel = Channels.newChannel(url.openStream());
      File fileOut = new File(fileName);
      fos = new FileOutputStream(fileOut);
      FileChannel writeChannel = fos.getChannel();
      if (withInfo) {
        String downloadName = "x";
        String t [] = fileName.split("/");
        downloadName = t[t.length-1];
        int downloadNameLen = downloadName.length();
        // erreur.println(downloadName.substring(downloadNameLen-4,downloadNameLen));
        if(downloadNameLen>4 && ".zip".equals(downloadName.substring(downloadNameLen-4,downloadNameLen))){
          downloadName = downloadName.substring(0,downloadNameLen-4);
        }
        //launch Thread that update %age of download
        downloadThread = new DownloadThread(fileOut, fileToDowloadSize, downloadName, progression);
        downloadThread.start();
      }
      // TODO #440 stop transferFrom if download take to long
      writeChannel.transferFrom(readChannel, 0, Long.MAX_VALUE);
      boolean completed = false;
      // try { //from javadoc to stop a transferFrom.
      //   begin();
      //   completed = ...;    // Perform blocking I/O operation
      //   return ...;         // Return result
      // } finally {
      //   end(completed);
      // }
      completed=true;
      return completed;
    // } catch (MalformedURLException e) {
    //   reason = "URL is malformed";
    // } catch (UnknownHostException e) {
    //   reason = "can't resolve host";
    // } catch (FileNotFoundException e) {
    //   reason = "file can't be found on the web site";
    //   ex=e;
    } catch (Exception e) {
      // reason = e.toString();
      throw e;
    } finally {
      if(fos!=null){
        try {
          fos.close();
        }catch (IOException e) {
          erreur.alerte("Can't close FileOutputStream");
        }
      }
      if(downloadThread!=null){
        downloadThread.stopRuning();
      }
    }
  }
  /**
  *{@summary Download a file from the web.}<br>
  *@param urlPath the url as a String
  *@param fileName the name of the file were to save data from the web
  *@param withInfo if true launch a thread to have info during download
  *@lastEditedVersion 2.28
  */
  public static boolean download(String urlPath, String fileName, boolean withInfo){
    try {
      return download2(urlPath, fileName, withInfo);
    }catch (Exception e) {
      return false;
    }
  }
  /**
  *{@summary Download a file from the web.}<br>
  *@param urlPath the url as a String
  *@param fileName the name of the file were to save data from the web
  *@lastEditedVersion 2.28
  */
  public static boolean download(String urlPath, String fileName){
    return download(urlPath, fileName, false);
  }
  /**
  *{@summary return the size of the downloaded file.}
  *@lastEditedVersion 2.7
  */
  private static long getFileSize(URL url) {
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("HEAD");
      return conn.getContentLengthLong();
    } catch (IOException e) {
      erreur.erreur("fail to get file size");
      return -1;
    } finally { //will be call even if there is return before.
      if (conn != null) {
        conn.disconnect();
      }
    }
  }
  /**
  *{@summary a class to zip file.}<br>
  *cf https://www.baeldung.com/java-compress-and-uncompress
  *@lastEditedVersion 1.46
  */
  public static void zip(String sourceFolder, String outputFile){
    try {
      outputFile = str.addALaFinSiNecessaire(outputFile,".zip");
      FileOutputStream fos = new FileOutputStream(outputFile);
      ZipOutputStream zipOut = new ZipOutputStream(fos);
      File fileToZip = new File(sourceFolder);
      zipFile(fileToZip, fileToZip.getName(), zipOut, outputFile);
      zipOut.close();
      fos.close();
    }catch (Exception e) {
      erreur.erreur("Fail to zip file "+sourceFolder+" into "+outputFile);
    }
  }
  /**
  *{@summary a class to do main part of ziping a file.}<br>
  *cf https://www.baeldung.com/java-compress-and-uncompress
  *@lastEditedVersion 1.46
  */
  private static void zipFile(File fileToZip, String fileName, ZipOutputStream zipOut, String outputFile) {
    try {
      if (fileToZip.isHidden()) {
          return;
      }
      if (fileToZip.isDirectory()) {
          fileName = str.addALaFinSiNecessaire(fileName,"/");
          zipOut.putNextEntry(new ZipEntry(fileName));
          zipOut.closeEntry();
          File[] children = fileToZip.listFiles();
          for (File childFile : children) {
              zipFile(childFile, fileName + childFile.getName(), zipOut, outputFile);
          }
          return;
      }
      FileInputStream fis = new FileInputStream(fileToZip);
      ZipEntry zipEntry = new ZipEntry(fileName);
      zipOut.putNextEntry(zipEntry);
      byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
      }
      fis.close();
    }catch (Exception e) {
      erreur.erreur("Fail to zip file during ziping of "+fileToZip.getName()+" into "+outputFile);
    }
  }
  /**
  *{@summary Unzip a zip file.}<br>
  *cf https://www.baeldung.com/java-compress-and-uncompress
  *@param fileName the name of the .zip file.
  *@param folderName the name of the folder were to save data from the .zip.
  *@lastEditedVersion 2.28
  */
  public static void unzip(String fileName, final String folderName){
    fileName = str.addALaFinSiNecessaire(fileName,".zip");
    final File destDir = new File(folderName);
    destDir.mkdirs(); //add folder if needed.
    try {
      final ZipInputStream zis = new ZipInputStream(new FileInputStream(fileName));
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        createZipEntry(zipEntry, destDir, zis, ".");
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      zis.close();
    }catch (Exception e) {
      erreur.erreur("Fail to unzip "+fileName+" in "+folderName);
    }
  }
  /**
  *{@summary Create a file for a zip entry.}
  *If file need to have it's parent folder created, it will do that to.<br>
  *cf https://www.baeldung.com/java-compress-and-uncompress
  *@param zipEntry the zip entry to unzip
  *@param folderName destDir the folder where to create it
  *@param ZipInputStream the stream where to get content of the ZipEntry
  *@param folderInURL the folder to unzip from the url to exclude some folder of full path
  *@lastEditedVersion 2.28
  */
  private static void createZipEntry(ZipEntry zipEntry, File destDir, ZipInputStream zis, String folderInURL) throws IOException {
    final byte[] buffer = new byte[1024];
    final File newFile = newFile(destDir, zipEntry, folderInURL);
    if (zipEntry.isDirectory()) {
      if (!newFile.isDirectory() && !newFile.mkdirs()) {
        throw new IOException("Failed to create directory " + newFile);
      }
    } else {
      File parent = newFile.getParentFile();
      if (!parent.isDirectory() && !parent.mkdirs()) {
        throw new IOException("Failed to create directory " + parent);
      }
      // if(!setMaxPerm(parent)){erreur.erreur("zip entry parent/ perm failed to be set");}

      final FileOutputStream fos = new FileOutputStream(newFile);
      int len;
      while ((len = zis.read(buffer)) > 0) {
        fos.write(buffer, 0, len);
      }
      fos.close();
    }
    // if(!setMaxPerm(newFile)){erreur.erreur("zip entry perm failed to be set");}
  }
  /**
  *{@summary Download and unzip a .zip from the web.}<br>
  *With folderInURL we can download only part of the zip.<br>
  *Use this insted of download zip file and then unzip it will save performance.
  *Because .zip file is never save on the disc, only in RAM.<br>
  *@param url the zip url
  *@param folderName destDir the folder where to create it
  *@param folderInURL the folder to unzip from the url to exclude some folders &#38; files
  *@lastEditedVersion 2.28
  */
  public static boolean downloadAndUnzip(final String url, final String folderName, final String folderInURL){
    File destDir = new File(str.sToDirectoryName(folderName));
    destDir.mkdirs();
    // if(!setMaxPerm(destDir)){erreur.erreur("zip entry root perm failed to be set");}
    try {
      ZipInputStream zis = new ZipInputStream(new URL(url).openStream());
      ZipEntry entry;
      while((entry = zis.getNextEntry())!=null){
        if(entry.getName().startsWith(folderInURL)){
          if(getProgression()!=null){getProgression().setDownloadingMessage(entry.getName());}
          createZipEntry(entry, destDir, zis, folderInURL);
        }
        zis.closeEntry();
      }
      zis.close();
      return true;
    }catch (Exception e) {
      erreur.erreur("Fail to unzip "+url+" in "+folderName+" because of "+e);
      return false;
    }
  }
  /**
  *{@summary Download and unzip a .zip from the web.}<br>
  *Use this insted of download zip file and then unzip it will save performance.
  *Because .zip file is never save on the disc, only in RAM.<br>
  *@param url the zip url
  *@param folderName destDir the folder where to create it
  *@lastEditedVersion 2.28
  */
  public static boolean downloadAndUnzip(final String url, final String folderName){
    return downloadAndUnzip(url, folderName, ".");
  }
  /**
  *{@summary Count entry of a zip file.}<br>
  *@param url the zip url
  *@lastEditedVersion 2.29
  */
  public static int countEntryOfZipFile(final String url){
    try {
      ZipInputStream zis = new ZipInputStream(new URL(url).openStream());
      int cpt=0;
      ZipEntry entry;
      while((entry = zis.getNextEntry())!=null){
        if(!entry.isDirectory()){
          cpt++;
        }
      }
      return cpt;
    }catch (Exception e) {
      erreur.alerte("Fail to count entry of zip file because of "+e);
      return -1;
    }
  }
  /**
  *{@summary A safe way to create a File from a zip file to avoid Zip Slip.}<br>
  *@param destinationDir File that we whant to create in the zipEntry folder.
  *@param zipEntry the ZipEntry.
  *@lastEditedVersion 2.28
  */
  public static File newFile(File destinationDir, ZipEntry zipEntry, String folderInURL) throws IOException {
    String zipEntryName = zipEntry.getName();
    if(zipEntryName.startsWith(folderInURL)){
      zipEntryName=zipEntryName.substring(folderInURL.length()-1,zipEntryName.length());
    }
    File destFile = new File(destinationDir, zipEntryName);
    String destDirPath = destinationDir.getCanonicalPath();
    String destFilePath = destFile.getCanonicalPath();
    if (!destFilePath.startsWith(destDirPath)) {
      throw new IOException("Entry "+destFilePath+" is outside of the target dir"+destDirPath);
    }
    return destFile;
  }

  /**
  *{@summary Set all the permissions for a file.}<br>
  *@param f file to set permissions
  *@lastEditedVersion 2.28
  */
  public static boolean setMaxPerm(File f){
    boolean itWork=f.setExecutable(true, false)
        & f.setReadable(true, false)
        & f.setWritable(true, false);
    erreur.info("set max perm to "+f+" work:"+itWork);
    return itWork;
  }
  /**
  *{@summary Set recursively all the permissions for a file.}<br>
  *@param f file to set permissions recursively
  *@lastEditedVersion 2.28
  */
  public static boolean setMaxPermRecursively(File f){
    Liste<File> parents = new Liste<File>();
    File fParent=f;
    while(fParent!=null){
      parents.addHead(fParent);
      fParent=fParent.getParentFile();
    }
    boolean itWork=true;
    for (File fi : parents) {
       itWork=itWork & setMaxPerm(fi);
    }
    return itWork;
  }
  /**
  *{@summary A safe way to launch a web page.}<br>
  *@param url the URL to open
  *@lastEditedVersion 2.21
  */
  public static boolean openWebLink(String url){
    try {
      return openURI(new URI(url));
    }catch (java.net.URISyntaxException e) {
      erreur.alerte("Fail to open malformed URI "+url);
      return false;
    }
  }
  /**
  *{@summary a safe way to open an URI.}<br>
  *@param uri the URI to open
  *@lastEditedVersion 2.21
  */
  public static boolean openURI(URI uri){
    if(uri==null){return false;}
    try {
      Desktop.getDesktop().browse(uri);
      return true;
    }catch (IOException e) {
      erreur.alerte("Fail to open URI "+uri);
      return false;
    }
  }
}

/**
*{@summary Print info about curent download.}<br>
*this thread watch file size &#38; print it / fileSize.
*@lastEditedVersion 2.7
*@author Hydrolien
*/
class DownloadThread extends Thread {
  private File fileOut;
  private long fileToDowloadSize;
  private boolean running;
  private String downloadName;
  private Progression progressionInstance;
  /**
  *{@summary Main constructor.}<br>
  *@param fileOut file that we are curently filling by the downloading file
  *@param fileToDowloadSize size that we should reach when download will end
  *@lastEditedVersion 2.7
  */
  public DownloadThread(File fileOut, long fileToDowloadSize, String downloadName, Progression progressionInstance){
    this.fileOut = fileOut;
    this.fileToDowloadSize = fileToDowloadSize;
    this.downloadName=downloadName;
    this.progressionInstance=progressionInstance;
    running=true;
  }

  public void stopRuning(){running=false;}
  /**
  *{@summary Main function that print every second %age of download done.}<br>
  *@lastEditedVersion 2.7
  */
  public void run(){
    long fileOutSize=0;
    long lastFileOutSize=0;
    long timeStart=System.currentTimeMillis();
    long timeFromLastBitDownload=timeStart;
    while (fileOutSize < fileToDowloadSize && running) {
      fileOutSize = fileOut.length();
      double progression = ((double)fileOutSize)/(double)fileToDowloadSize;
      int percent = (int)(100*progression);
      long curentTime = System.currentTimeMillis();
      long timeElapsed = curentTime-timeStart;
      long timeLeft = (long)((double)((timeElapsed/progression)-timeElapsed));
      String sTimeLeft = Time.msToTime(timeLeft)+" left";
      String message = "Downloading "+downloadName+" - "+percent+"% - ";
      if(fileOutSize!=lastFileOutSize){//update watcher of working download
        timeFromLastBitDownload=curentTime;
      }
      if(timeFromLastBitDownload+10000<curentTime){
        message+=(((curentTime-timeFromLastBitDownload)/1000)+"s untill a new bit haven't been download");
        if(timeFromLastBitDownload+60000<curentTime){
          erreur.erreur("STOP download");
          stopRuning();
          //TODO #440 stop download.
        }
      }else{
        message+=sTimeLeft;
      }
      progressionInstance.setDownloadingValue(percent);
      progressionInstance.setDownloadingMessage(message);

      lastFileOutSize=fileOutSize;
      Time.pause(50);
    }
    erreur.info("download done");
  }
}
