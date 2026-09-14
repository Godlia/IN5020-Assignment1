package com.group2.client;

import java.io.File;
import java.io.FileNotFoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

import com.group2.server.ServerInterface;

public class Client {
    public static void main(String[] args) {
        Scanner stdinScanner = new Scanner(System.in);
        System.out.println("Input file path to queryset: ");
        String filePath = stdinScanner.nextLine();
        File queryFile = new File(filePath);
        Registry registry = null;
        ServerInterface server = null;
        

        try {
            registry = LocateRegistry.getRegistry();
            server = (ServerInterface) registry.lookup("server");
            if(!server.status()) throw new RemoteException("Server responded NOK");
        } catch (RemoteException | NotBoundException e) {
            e.printStackTrace();
        }



        try(Scanner fileScanner = new Scanner(queryFile)) {
            while(fileScanner.hasNextLine()) {
                //logic for running through file and decoding function calls & parameters
                server.status();
            }
        } catch (FileNotFoundException | RemoteException e) {
            e.printStackTrace();
        }
    }



}
