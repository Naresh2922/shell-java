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

        System.out.print("$ ");
        try(Scanner scanner = new Scanner(System.in)){
            while(true){
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

                if (arguments.contains("2>")) {
                    String[] parts = arguments.split(">", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                    redirectOperator = "2>";
                } else if (arguments.contains("1>")) {
                    String[] parts = arguments.split("1>", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                    redirectOperator = "1>";
                } else if (arguments.contains(">")) {
                    String[] parts = arguments.split(">", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                    redirectOperator = ">";
                }

                switch(command){
                    case "exit": 
                        exit(arguments);
                        break;
                    case "echo":
                        List<String> tokens = getTokens(arguments);
                        if(!redirectOperator.isBlank()){
                            executeNonBuiltInCommand(command, directories, arguments, redirectOperator, redirectionFile);
                            break;
                        }
                        System.out.println(String.join(" ", tokens));
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
                        executeNonBuiltInCommand(command, directories, arguments, redirectOperator, redirectionFile);
                        break;
                }
                System.out.print("$ ");
            }
        }
    }

    private static List<String> getTokens(String inputString){
        char quote = '-';
        List<Character> escapes = Arrays.asList('\"', '$', '`', ' ', '\\');
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
                quote = '-';
                inQuote = false;
            } else if (!inQuote && (character == '\'' || character == '\"') ) {
                quote = character;
                inQuote = true;
            } else if (character == ' ' && !inQuote){
                tokens.add(sb.toString());
                sb.setLength(0);
                while((++i < inputString.length()) && (character = inputString.charAt(i)) == ' '){
                    continue;
                }
                --i;
            } else {
                sb.append(character);
            }
        }
        if (!sb.isEmpty()) tokens.add(sb.toString());
        return tokens;
    }

    private static int executeNonBuiltInCommand(String command, String[] directories, String arguments, String redirectOperator, String redirectionFile) throws FileNotFoundException{
        String filePath = isFileExecutable(command, directories);
        if(filePath.isEmpty()) {
            System.err.println(command + ": command not found");
            return -1;
        } else {
            //getTokens(arguments).forEach(System.out::println);
            String[] argument = getTokens(arguments).toArray(new String[0]);

            String[] commandWithArguments = new String[argument.length + 1];
            commandWithArguments[0] = command;
            System.arraycopy(argument, 0, commandWithArguments, 1, argument.length);
            //if (!redirectOperator.isBlank()) {
            //    handleRedirection(redirectionFile, redirectOperator);
            //}
            int exitCode = Main.executeCommand(commandWithArguments, redirectionFile, redirectOperator);
            return exitCode;
        }
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

    private static int executeCommand(String[] arguments, String redirectionFile, String redirectOperator) throws FileNotFoundException {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(arguments);
            

            if (!redirectOperator.isBlank()) {
                if (redirectOperator.equals(">") || redirectOperator.equals("1>")) {
                    processBuilder.redirectOutput(new File(redirectionFile));
                    processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT); 
                } else if (redirectOperator.equals("2>")) {
                    processBuilder.redirectError(new File(redirectionFile)); 
                }
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                return exitCode;
            }

            Process process = processBuilder.start();
            
            try (BufferedReader bout = new BufferedReader(new InputStreamReader(process.getInputStream()));
                 BufferedReader berr = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                StringBuilder sbOut = new StringBuilder();
                StringBuilder sbErr = new StringBuilder();
                String line;
                
                while ((line = bout.readLine()) != null) {
                    sbOut.append(line).append(System.lineSeparator());
                }
                
                while ((line = berr.readLine()) != null) {
                    sbErr.append(line).append(System.lineSeparator());
                }
                
                if (sbOut.length() > 0) {
                    System.out.print(sbOut.toString());
                }
                if (sbErr.length() > 0) {
                    System.err.print(sbErr.toString());
                }
            } catch (IOException io) {
                io.printStackTrace();
            }
            
            int exitCode = process.waitFor();
            

            return exitCode;
        } catch (IOException | InterruptedException e) {
            System.err.println(arguments[0] + ": command not found");
            return -1;
        }
    }
}
