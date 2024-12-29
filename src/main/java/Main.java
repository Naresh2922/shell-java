import java.util.List;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.nio.file.Paths;
import java.nio.file.Path;

public class Main {
    private static final List<String> commandsList = List.of("type", "exit", "echo");
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        String path = System.getenv("PATH");
        String[] directories = path.split(System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":");
        
        try(Scanner scanner = new Scanner(System.in)){
            while(true){
                System.out.print("$ ");
                String input = scanner.nextLine().trim();
                String[] inputArray = input.split(" ", 2);
                String command = inputArray[0].trim();
                String arguments = inputArray.length > 1 ? inputArray[1].trim() : ""; 
                switch(command){
                    case "exit" : 
                        exit(arguments);
                        break;
                    case "echo" : {
                        System.out.print(arguments + System.lineSeparator());
                        break;
                    }
                    case "type" :
                        type(arguments, directories);
                        break;
                    default :
                        String filePath = isCommandExist(command, directories);
                        if(filePath.equals("")) System.err.println(input + ": command not found");
                        else {
                            String[] argument = arguments.split(" ");
                            String[] commandWithArguments = new String[argument.length + 1];
                            commandWithArguments[0] = command;
                            System.arraycopy(argument, 0, commandWithArguments, 1, argument.length);
                            executeCommand(commandWithArguments);
                        }
                        break;
                }
            }         
        }       
    }


    private static void type(String arguments, String[] directories){
        if(arguments.equals("")) return;
            if(commandsList.contains(arguments)){
                System.out.println(arguments + " is a shell builtin");
                return;
            }
        String filePath = isCommandExist(arguments, directories)
        if(filePath.equals("")) System.out.println(arguments + ": not found");
        else System.out.println(arguments + " is " + filePath);
    }

    private static void exit(String arguments) {
        if(!arguments.equals("")){
            try{
                if(Integer.parseInt(arguments) == 0){
                    System.exit(0);
                }
            } catch (NumberFormatException nfe){
                System.err.println("Expected format -- exit <integer> :" + nfe.getMessage());
            }
            
        } else {
            System.err.println("Expected format -- exit <integer> -- exit 0 for termination");
        }
    }

    private static String isCommandExist(String arguments, String[] directories){
        for(String s : directories){
            if(!Files.exists(Paths.get(s)) && !Files.isDirectory(Paths.get(s))) continue;
            try(Stream<Path> files = Files.walk(Paths.get(s))){
                boolean fileFound = files.map(filePath -> filePath.getFileName())
                                        .anyMatch(fileName -> fileName != null && fileName.toString().equals(arguments));
                if(fileFound) {
                    return (s + (System.getProperty("os.name").toLowerCase().contains("win") ? "\\" : "/") + arguments);
                }
            } catch (IOException io){
                System.err.println("Exception occured while iterating through directories " + io.getMessage());
                io.printStackTrace();
            }
        }
        return "";
    }

    private static void executeCommand(String[] arguments){
        try {
            ExecutorService executorService =  Executors.newFixedThreadPool(2);

            Process process = Runtime.getRuntime().exec(arguments);

            executorService.submit(() -> {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                    String line;
                    while((line = br.readLine()) != null){
                        System.out.println(line);
                    }
                } catch (IOException io){
                    io.printStackTrace();
                }
            });

            executorService.submit(() -> {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))){
                    String line;
                    while((line = br.readLine()) != null){
                        System.out.println(line);
                    }
                } catch (IOException io){
                    io.printStackTrace();
                }
            });

            int code = process.waitFor();
            executorService.shutdown();
            executorService.awaitTermination(30, TimeUnit.SECONDS);
            System.out.println("Exited with code : " +  code);
        } catch (InterruptedException | IOException ioe) {
            System.err.println("process Interrupted : " + ioe.getMessage());
            ioe.printStackTrace();
        }
    }
}
