package com.itu.os.cache.coherence.datastore;

import com.itu.os.cache.coherence.dto.FileServiceAckDTO;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum DataStore {

    INSTANCE;

    /**
     * It holds the mapping of clients that holds a copy of a file
     */
    private static final Map<String, Set<String>> fileToClientsMapping = new HashMap<>();
    /**
     * It holds a mapping of clients from which ack is expected for a file notification
     */
    private static final Map<String, Set<String>> notificationAckPendingMapping = new HashMap<>();
    /**
     * It holds a mapping of list of file for which upload/update ack is pending
     */
    private static final Map<String, FileServiceAckDTO> ackPendingMapping = new HashMap<>();

    public synchronized boolean containsFile(String fileName){
        return fileToClientsMapping.containsKey(fileName);
    }

    public synchronized Set<String> addEntryToFileToClientMapping(String fileName, String clientName){
        Set<String> clients = fileToClientsMapping.getOrDefault(fileName, new HashSet<>());
        clients.add(clientName);
        return fileToClientsMapping.put(fileName, clients);
    }

    public synchronized Set<String> removeEntryFromFileToClientMapping(String fileName){
        return fileToClientsMapping.remove(fileName);
    }

    public synchronized Set<String> addEntryToNotificationAckPendingMapping(String filename, Set<String> clientNames){
        return notificationAckPendingMapping.put(filename, clientNames);
    }

    public synchronized boolean removeEntryFromNotificationAckPendingMapping(String fileName, String clientName){
        Set<String> clients = notificationAckPendingMapping.getOrDefault(fileName, new HashSet<>());
        boolean deleteStatus = clients.remove(clientName);
        if(clients.size() == 0){
            notificationAckPendingMapping.remove(fileName);
        }else{
            notificationAckPendingMapping.put(fileName, clients);
        }
        return deleteStatus;
    }


    public synchronized int sizeOfNotificationAckPendingMapping(String fileName){
        if(notificationAckPendingMapping.containsKey(fileName)){
            notificationAckPendingMapping.get(fileName).size();
        }
        return 0;
    }

    public synchronized FileServiceAckDTO addAEntryToAckPendingMapping(String fileName, FileServiceAckDTO fileServiceAckDTO){
        return ackPendingMapping.put(fileName, fileServiceAckDTO);
    }

    public synchronized FileServiceAckDTO removeEntryFromAckPendingMapping(String fileName){
        return ackPendingMapping.remove(fileName);
    }
}
