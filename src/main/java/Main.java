import java.io.*;
import java.nio.file.DirectoryStream;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.nio.file.Paths;
import java.nio.file.Path;


public class Main {
    private static final Set<String> commandList = Set.of("type", "exit", "echo", "pwd", "cd");
    static String path = System.getenv("PATH");
    static String home = System.getenv("HOME");
    static String currentWorkingDirectory = Paths.get("").toAbsolutePath().toString();
    static File pseudoDirectory = new File(currentWorkingDirectory);
    
    public static void main(String[] args) throws Exception {
        
        String[] directories = Main.path.split(System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":");

        try(Scanner scanner = new Scanner(System.in)){
            while(true){
                System.setOut(System.out);
                System.setErr(System.err);
                System.out.print("$ ");
                String input = scanner.nextLine().trim();
                String[] inputArray;
                String command;
                String arguments;
                String redirectionFile = "";
                String redirectOperator = "";
                
                if(input.startsWith("'") || input.startsWith("\"")){
                    char c = input.charAt(0);
                    command = input.substring(1, input.indexOf(c, 1));
                    arguments = input.substring(input.indexOf(c, 1) + 1).trim();
                } else{
                    inputArray = input.split("\\s+", 2);
                    command = inputArray[0].trim();
                    arguments = inputArray.length > 1 ? inputArray[1].trim() : "";
                }

                if (arguments.contains(">")) {
                    String[] parts = arguments.split(">", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                    redirectOperator = ">";
                } else if (arguments.contains("1>")) {
                    String[] parts = arguments.split("1>", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                    redirectOperator = "1>";
                } else if (arguments.contains("2>")) {
                    String[] parts = arguments.split("2>", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                    redirectOperator = "2>";
                }

                switch(command){
                    case "exit": 
                        exit(arguments);
                        break;
                    case "echo":
                        List<String> tokens = getTokens(arguments);
                        if(!redirectOperator.isBlank()){
                            handleRedirection(redirectionFile, redirectOperator);
                        }
                        System.out.println(String.join("", tokens));
                        System.setOut(System.out);
                        System.setErr(System.err);
                        break;
                    case "type":
                        type(arguments, directories);
                        break;
                    case "pwd":
                        System.out.println(Main.pseudoDirectory.getAbsolutePath());
                        break;
                    case "cd":
                        cd(arguments);
                        break;
                    default:
                        String filePath = isFileExecutable(command, directories);
                        if(filePath.isEmpty()) {
                            System.err.println(command + ": command not found");
                        } else {
                            String[] argument = getTokens(arguments).toArray(new String[0]);
                            String[] commandWithArguments = new String[argument.length + 1];
                            commandWithArguments[0] = command;
                            System.arraycopy(argument, 0, commandWithArguments, 1, argument.length);
                            if (!redirectOperator.isBlank()) {
                                handleRedirection(redirectionFile, redirectOperator);
                            }
                            int exitCode = executeCommand(commandWithArguments, redirectionFile, redirectOperator);
                        }
                        break;
                }
            }
        }
    }

    private static List<String> getTokens(String inputString){
        char quote = '-';
        List<Character> escapes = Arrays.asList('\"', '$', '`', ' ', '\\', 'n');
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < inputString.length(); i++) {
            char character = inputString.charAt(i);
            if('\\' == character ){
                if((++i < inputString.length()) && escapes.contains(character = inputString.charAt(i))){
                    if(inQuote && (quote == '\'')){
                        sb.append('\\');
                        if(quote != character) sb.append(character);
                        continue;
                    }
                    sb.append(character);
                } else if (!inQuote){
                    sb.append(character);
                }else {
                    sb.append('\\');
                    sb.append(character);
                }
            } else if (character == quote) {
                tokens.add(sb.toString());
                sb.setLength(0);
                quote = '-';
                inQuote = false;
            } else if (!inQuote && (character == '\'' || character == '\"') ) {
                quote = character;
                inQuote = true;
            } else if (character == ' ' && !inQuote){
                while((++i < inputString.length()) && (character = inputString.charAt(i)) == ' '){
                    continue;
                }
                --i;
                sb.append(' ');
            } else {
                sb.append(character);
            }
        }
        if (!sb.isEmpty()) tokens.add(sb.toString());
        return tokens;
    }

    private static void type(String arguments, String[] directories){
        if(arguments.isEmpty()) return;
        if(commandList.contains(arguments)){
            System.out.println(arguments + " is a shell builtin");
            return;
        }
        String filePath = isFileExecutable(arguments, directories);
        if(filePath.isEmpty()) System.out.println(arguments + ": not found");
        else System.out.println(arguments + " is " + filePath);
    }

    private static void exit(String arguments) {
        if(!arguments.isEmpty()){
            try{
                if(Integer.parseInt(arguments) == 0){
                    System.exit(0);
                }
            } catch (NumberFormatException nfe){
                System.err.println("Usage -- exit <integer> :" + nfe.getMessage());
            }

        } else {
            System.err.println("Usage -- exit <integer> -- exit 0 for termination");
        }
    }

    private static void cd(String arguments) {
        if(arguments.equals("/") || arguments.equals("")) return;
        String[] arrayArguments = arguments.split(" ");
        for(String arg : arrayArguments){
            if(arg.matches("^(\\.\\./)+$")) {
                int count = (int) arg.chars().filter(c -> c == '/').count();
                while(count != 0){
                    Main.pseudoDirectory = Main.pseudoDirectory.getParentFile();
                    count--;
                }
            } else if (arg.startsWith("./")){
                try{
                    Main.pseudoDirectory = new File(Main.pseudoDirectory.getAbsolutePath() + "//" + arg).getCanonicalFile();
                } catch (IOException io) {
                    io.printStackTrace();
                }
            } else if (arg.equals("~")){
                Main.pseudoDirectory = new File(Main.home);
            } else if (arg.matches("/[^/]+")) {
                if(Files.exists(Paths.get(arg)) && Files.isDirectory(Paths.get(arg))){
                    Main.pseudoDirectory = new File(arg);
                } else {
                    System.out.println("cd: "+ arg + ": No such file or directory");
                }
            } else {
                if(Files.exists(Paths.get(arg)) && Files.isDirectory(Paths.get(arg))){
                    pseudoDirectory = new File(arg);
                } else {
                    System.out.println("cd: "+ arg + ": No such file or directory");
                }
            }
        }
    }

    private static String isFileExecutable(String command, String[] directories){
        for(String s : directories){
            if(!Files.exists(Paths.get(s)) && !Files.isDirectory(Paths.get(s))) continue;
            try(Stream<Path> files = Files.walk(Paths.get(s))){
                boolean fileFound = files.map(filePath -> filePath.getFileName())
                                       .anyMatch(fileName -> fileName != null && fileName.toString().equals(command));
                if(fileFound) {
                    return (s + (System.getProperty("os.name").toLowerCase().contains("win") ? "\\" : "/") + command);
                }
            } catch (IOException io){
                System.err.println("Exception occurred while iterating through directories " + io.getMessage());
                io.printStackTrace();
            }
        }
        return "";
    }

    private static void handleRedirection(String redirectionFile, String redirectOperator) {
        if (redirectOperator.equals(">") || redirectOperator.equals("1>")) {
            try {
                System.setOut(new PrintStream(new FileOutputStream(redirectionFile)));
            } catch (FileNotFoundException fnf) {
                fnf.printStackTrace();
            }
        } else if (redirectOperator.equals("2>")) {
            try {
                System.setErr(new PrintStream(new FileOutputStream(redirectionFile)));
            } catch (FileNotFoundException fnf) {
                fnf.printStackTrace();
            }
        }
    }

    private static int executeCommand(String[] arguments, String redirectionFile, String redirectOperator) throws FileNotFoundException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(arguments);

            // Handle redirection if needed
            if (!redirectOperator.isBlank()) {
                handleRedirection(redirectionFile, redirectOperator);
            }

            // Start the process
            Process process = processBuilder.start();

            // Collect output and error streams
            try (BufferedReader bout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader berr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder sbOut = new StringBuilder();
                StringBuilder sbErr = new StringBuilder();
                String line;

                // Read the standard output
                while ((line = bout.readLine()) != null) {
                    sbOut.append(line).append(System.lineSeparator());
                }

                // Read the standard error
                while ((line = berr.readLine()) != null) {
                    sbErr.append(line).append(System.lineSeparator());
                }

                // Print standard output and error if no redirection
                if (sbOut.length() > 0) {
                    System.out.print(sbOut.toString());
                }
                if (sbErr.length() > 0) {
                    System.err.print(sbErr.toString());
                }
            } catch (IOException io) {
                io.printStackTrace();
            }

            // Wait for process to complete
            int exitCode = process.waitFor();

            // Reset the System.out and System.err streams after the redirection
            if (!redirectOperator.isBlank()) {
                System.setOut(System.out);
                System.setErr(System.err);
            }

            // Print the prompt after command execution
            System.out.print("$ ");  // Ensure the prompt is printed after execution

            return exitCode;
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during command execution: " + e.getMessage());
            return -1;
        }
    }

}
