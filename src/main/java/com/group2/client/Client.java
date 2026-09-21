package com.group2.client;

import java.io.File;
import java.lang.reflect.Method;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
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



        try {
            Scanner fileScanner = new Scanner(queryFile);
            while(fileScanner.hasNextLine()) {
                //logic for running through file and decoding function calls & parameters
                String line = fileScanner.nextLine();
                // Exercise explicitly says the line will have 1 methodname and a singular argument, simplifying the string parsing
                String[] stringArr = line.split(" ");
                Object[] callArgs = Arrays.copyOfRange(stringArr, 1, stringArr.length);
                System.out.println(Arrays.toString(stringArr) + Arrays.toString(callArgs));
                Method requestedMethod = ServerInterface.class.getMethod(stringArr[0]);

                Object response = requestedMethod.invoke(server, callArgs);
                System.out.println(response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
