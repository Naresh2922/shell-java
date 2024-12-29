import java.util.Scanner;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.nio.file.Paths;
import java.nio.file.Path;

public class Main {
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
                    case "exit" : {
                        if(!arguments.equals("")){
                            try{
                                if(Integer.parseInt(inputArray[1]) == 0){
                                    System.exit(0);
                                }
                            } catch (NumberFormatException nfe){
                                System.err.println("Expected format -- exit <integer> :" + nfe.getMessage());
                            }
                            
                        } else {
                            System.err.println("Expected format -- exit <integer> -- exit 0 for termination");
                        }
                        break;
                    }

                    case "echo" : {
                        System.out.print(arguments + System.lineSeparator());
                        break;
                    }

                    case "type" :{
                        if(arguments.equals("")) break;
                        boolean fileFound = false;
                        for(String s : directories){
                            if(!Files.exists(Paths.get(s)) && !Files.isDirectory(Paths.get(s))) continue;
                            try(Stream<Path> files = Files.walk(Paths.get(s))){
                                fileFound = files.map(filePath -> filePath.getFileName())
                                                        .anyMatch(fileName -> fileName != null && fileName.toString().equals(arguments));
                                if(fileFound) {
                                    System.out.println(arguments + " is " + s + (System.getProperty("os.name").toLowerCase().contains("win") ? "\\" : "/") + arguments);
                                    break;
                                }
                            } catch (IOException io){
                                System.err.println("Exception occured while iterating through directories " + io.getMessage());
                                io.printStackTrace();
                            }
                        }
                        if(!fileFound) System.out.println(arguments + ": not found");
                    }
                    break;
                    default : 
                        System.err.println(input + ": command not found");
                        break;
                }
            }         
        }       
    }
}
