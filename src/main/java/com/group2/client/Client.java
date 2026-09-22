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
                String[] stringArr = line.split("\\s+");
                Method requestedMethod = findMethod(stringArr[0]);
                Object[] callArgs = parseArguments(requestedMethod, stringArr);
                System.out.println(Arrays.toString(stringArr) + Arrays.toString(callArgs));

                Object response = requestedMethod.invoke(server, callArgs);
                System.out.println(response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Method findMethod(String methodName) {
        return Arrays.stream(ServerInterface.class.getMethods())
                .filter(method -> method.getName().equals(methodName))
                .findFirst()
                .orElseThrow();
    }

    private static Object[] parseArguments(Method method, String[] tokens) {
        int end = tokens.length - 1;
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] callArgs = new Object[parameterTypes.length];

        if (parameterTypes.length == 1) {
            callArgs[0] = String.join(" ", Arrays.copyOfRange(tokens, 1, end));
        } else if (parameterTypes[0] == String.class) {
            callArgs[0] = String.join(" ", Arrays.copyOfRange(tokens, 1, end - 2));
            callArgs[1] = Integer.parseInt(tokens[end - 2]);
            callArgs[2] = tokens[end - 1];
        } else {
            for (int index = 0; index < parameterTypes.length; index++) {
                callArgs[index] = parameterTypes[index] == int.class
                        ? Integer.parseInt(tokens[index + 1])
                        : tokens[index + 1];
            }
        }
        return callArgs;
    }

}