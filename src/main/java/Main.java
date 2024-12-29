import java.util.Scanner;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage

        List<String> commandsList = List.of("echo", "exit", "type");

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
                        boolean found = false;
                        for(String s : directories){
                            if(s.trim().endsWith(arguments)) {
                                System.out.println(arguments + " is " + s);
                                found = true;
                                break;
                            };
                        }
                        if(!found) System.out.println(arguments + ": not found");
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
