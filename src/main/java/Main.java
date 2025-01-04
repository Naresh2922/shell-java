import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.stream.Stream;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.io.FileReader;

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
                System.out.print("$ ");
                String input = scanner.nextLine().trim();
                String[] inputArray = input.split("\\s+", 2);
                String command = inputArray[0].trim();
                String arguments = inputArray.length > 1 ? inputArray[1].trim() : "";
                switch(command){
                    case "exit" : 
                        exit(arguments);
                        break;
                    case "echo" : {
                        List<String> tokens  = getTokens(arguments);
                        System.out.println(String.join("", tokens));
                        break;
                    }
                    case "type" :
                        type(arguments, directories);
                        break;
                    case "pwd" :
                        System.out.println(Main.pseudoDirectory.getAbsolutePath());
                        break;
                    case "cd" :
                        cd(arguments);
                        break;
                    case "cat" :
                        String reg =  null;
                        if(arguments.startsWith("\"")) reg = "\"";
                        else reg = "'";
                        List<String> files = Arrays.stream(arguments.split(reg))
                                                                    .map(String::trim)
                                                                    .filter(s -> !s.isEmpty())
                                                                    .toList();
                        //files.forEach(System.out::println);
                        printContent(files);
                        break;
                    default :
                        String filePath = isFileExecutable(command, directories);
                        if(filePath.isEmpty()) System.err.println(command + ": command not found");
                        else {
                            String[] argument = arguments.split(" ");
                            String[] commandWithArguments = new String[argument.length + 1];
                            commandWithArguments[0] = command;
                            System.arraycopy(argument, 0, commandWithArguments, 1, argument.length);
                            int exitCode = executeCommand(commandWithArguments);

                        }
                        break;
                }
            }
        }
    }

    private static List<String> getTokens(String inputString){
        char quote = '-';
        List<Character> escapes = Arrays.asList('\'', '\"', '$', '`', ' ', '\\', 'n');
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < inputString.length(); i++) {
            char character = inputString.charAt(i);
            if('\\' == character ){
                if((++i < inputString.length()) && escapes.contains(character = inputString.charAt(i))){
                    if(inQuote && (quote == '\'' || quote == '\"')){
                        sb.append('\\');
                        if(quote != character) sb.append(character);
                        continue;
                    }
                    sb.append(character);
                } else {
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

    private static int executeCommand(String[] arguments){
        try {
            ExecutorService executorService =  Executors.newFixedThreadPool(2);

            Process process = Runtime.getRuntime().exec(arguments);

            CountDownLatch countDown = new CountDownLatch(2);

            executorService.submit(() -> {
                try(BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))){
                    String line;
                    while((line = br.readLine()) != null){
                        System.out.println(line);
                    }
                } catch (IOException io){
                    io.printStackTrace();
                } finally {
                    countDown.countDown();
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
                } finally {
                    countDown.countDown();
                }
            });

            countDown.await();
            int exit = process.waitFor();
            executorService.shutdown();
            return exit;


        } catch (InterruptedException | IOException ioe) {
            System.err.println("process Interrupted : " + ioe.getMessage());
            ioe.printStackTrace();
        }
        return 0;
    }

    private static void printContent(List<String> files){
        files.stream().forEach(file -> {
            if(Files.exists(Paths.get(file)) && Files.isReadable(Paths.get(file))){
                try(BufferedReader br = new BufferedReader(new FileReader(file))){
                    String line;
                    while((line = br.readLine()) != null){
                        System.out.print(line);
                    }
                } catch (IOException io){
                    io.printStackTrace();
                }
            }
        });
        System.out.println();
    }
}
