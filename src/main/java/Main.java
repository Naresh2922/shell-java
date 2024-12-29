import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        
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
                                System.err.println("format -- exit <integer> :" + nfe.getMessage());
                            }
                            
                        } else {
                            System.err.println("expected format -- exit <integer> -- exit 0 for termination");
                        }
                        break;
                    }

                    case "echo" : {
                        System.out.print(arguments);
                        System.out.print(System.lineSeparator());
                        break;
                    }

                    default : 
                        System.err.println(input + ": " + "command not found");
                        break;
                }
            }         
        }       
    }
}
