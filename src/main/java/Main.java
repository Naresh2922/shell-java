import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // Uncomment this block to pass the first stage
        
        try(Scanner scanner = new Scanner(System.in)){
            while(true){
                System.out.print("$ ");
                String input = scanner.nextLine();
                String[] inputArray = input.split(" ");
                if(inputArray[0].equals("exit") && inputArray.length == 2){
                    try{
                        if(Integer.parseInt(inputArray[1]) == 0){
                            System.exit(0);
                        }
                    } catch (NumberFormatException nfe){
                        System.err.println("format -- exit <integer> :" + nfe.getMessage());
                    }
                    
                }
                System.err.println(input + ": " + "command not found");
            }         
        }       
    }
}
