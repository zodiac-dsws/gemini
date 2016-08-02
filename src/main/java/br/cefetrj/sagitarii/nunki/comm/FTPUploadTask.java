package br.cefetrj.sagitarii.nunki.comm;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import br.cefetrj.sagitarii.nunki.ZipUtil;

public class FTPUploadTask implements Callable<Long> {
	private Logger logger = LogManager.getLogger( this.getClass().getName() );
	private String storageAddress;
	private int storagePort;
    private String user = "cache";
    private String password = "cache";
    private List<String> fileNames;
    private String targetTable;
    private String experimentSerial; 
	private String sessionSerial; 
	private String sourcePath;
	private int sendTry = 0;
	private final int TRY_LIMIT = 5;
	
	public FTPUploadTask(List<String> fileNames, String storageAddress, 
			int storagePort, String targetTable, String experimentSerial, 
			String sessionSerial, String sourcePath ) {
		this.fileNames = fileNames;
		this.storageAddress = storageAddress;
		this.storagePort = storagePort;
		this.targetTable = targetTable;
		this.experimentSerial = experimentSerial;
		this.sessionSerial = sessionSerial;
		this.sourcePath = sourcePath;
		logger.debug("create");
	}
	
	private long uploadFiles() {
		logger.debug("sending " + fileNames.size() + " files to table " + targetTable + " in session " +
					sessionSerial + " experiment " + experimentSerial + ": " + sourcePath );
		boolean hasError = false;
		long size = 0;
        FTPClient ftpClient = new FTPClient();
        try {
 
            ftpClient.connect(storageAddress, storagePort);
            ftpClient.login(user, password);
            logger.debug("FTP Response: " + ftpClient.getReplyString() );  
            
            ftpClient.setBufferSize(1048576);
            
			int indexFile = 1;
			for ( String fileName : fileNames ) {
				logger.debug("[" + indexFile + "] will send " + fileName );
				indexFile++;
			
				String newFileName = fileName + ".gz";
				
				logger.debug("compressing " + fileName + "...");
				ZipUtil.compress(fileName, newFileName);
				logger.debug("done compressing " + fileName + ".");
			
				File localFile = new File(newFileName);
				
				size = size + localFile.length();
				
		        logger.debug("sending [" + sessionSerial + "] " + localFile.getName() + " with size of " + localFile.length() + " bytes..." );
		        logger.debug("Strategy: FTP to " + user + "@" + storageAddress + ":" + storagePort);
			
	        	
	            ftpClient.enterLocalPassiveMode();
	            logger.debug("FTP Response: " +  ftpClient.getReplyString() );  
	            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
	            
	            String remoteFile = "/" + sessionSerial + "/" + localFile.getName();
	            logger.debug("FTP copy " + newFileName + " to ftp://" + storageAddress + ":" + storagePort + remoteFile);            
	            
	            InputStream inputStream = new FileInputStream( localFile );            
	            boolean done = ftpClient.storeFile( remoteFile, inputStream );
	            logger.debug("FTP Response: " + ftpClient.getReplyString() );  
	            
	            inputStream.close();
	            if ( done ) {
	            	logger.debug("File [" + sessionSerial + "] " + localFile.getName()+" is uploaded successfully.");
	            } else {
	            	logger.error("Cant upload the file [" + sessionSerial + "] " + localFile.getName() );
	            }

	            localFile.delete();
			}
            
        } catch ( Exception e ) {
        	logger.error("Error sending file by FTP: " + e.getMessage() );
        	hasError = true;
        }
        
    	try { 
    		ftpClient.disconnect();
        	logger.debug("FTP client disconnected.");
    	} catch ( Exception e ) { 
    		logger.error("cannot close FTP client: " + e.getMessage() );
    	}
    	
    	if( hasError && sendTry < TRY_LIMIT ) {
    		sendTry++;
    		logger.debug("try " + sendTry + ". will try to send again.");
    		uploadFiles();
    	}
        
        return size;
	}

	@Override
	public Long call() throws Exception {
		logger.debug("start");
		try {
			uploadFiles();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		return 0L;
	}
	
	
}
