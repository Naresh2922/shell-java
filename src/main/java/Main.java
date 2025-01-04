import java.io.*;
import java.nio.file.DirectoryStream;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;
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
                System.out.print("$ ");
                String input = scanner.nextLine().trim();
                String[] inputArray;
                String command ;
                String arguments;
                String redirectionFile = "";
                if(input.startsWith("'") || input.startsWith("\"")){
                    char c = input.charAt(0);
                    command = input.substring(1, input.indexOf(c, 1));
                    arguments = input.substring(input.indexOf(c, 1) + 1).trim();
                } else{
                    inputArray = input.split("\\s+", 2);
                    command = inputArray[0].trim();
                    arguments = inputArray.length > 1 ? inputArray[1].trim() : "";
                }

                if (arguments.contains(">")){
                    String[] parts = arguments.split(">", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                } else if (arguments.contains("1>")){
                    String[] parts = arguments.split("1>", 2);
                    arguments = parts[0].trim();
                    redirectionFile = parts[1].trim();
                }


                switch(command){
                    case "exit" : 
                        exit(arguments);
                        break;
                    case "echo" :
                        List<String> tokens  = getTokens(arguments);
                        if(!redirectionFile.isEmpty()){
                            try(BufferedWriter bw = new BufferedWriter(new FileWriter(redirectionFile))){
                                bw.write(String.join("", tokens));
                            } catch (IOException io){
                                io.printStackTrace();
                            }
                        } else {
                            System.out.println(String.join("", tokens));
                        }
                        break;
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
                        if(!redirectionFile.isEmpty()){
                            handleRedirection(files, redirectionFile, command);
                        } else {
                            printContent(files);
                        }
                        break;
                    default :
                        String filePath = isFileExecutable(command, directories);
                        if(filePath.isEmpty()) System.err.println(command + ": command not found");
                        else {
                            String[] argument = arguments.split(" ");
                            String[] commandWithArguments = new String[argument.length + 1];
                            commandWithArguments[0] = command;
                            System.arraycopy(argument, 0, commandWithArguments, 1, argument.length);
                            int exitCode = executeCommand(commandWithArguments, redirectionFile);

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
                    if(inQuote && (quote == '\'')){
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

    private static int executeCommand(String[] arguments, String redirectionFile){
        if(!redirectionFile.isEmpty()){
            List<String> files = new ArrayList<>();
            try(DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(arguments[1]))){
                for(Path entry : directoryStream){
                   files.add(arguments[1] + "/" + entry.getFileName().toString());
                }
                files.sort(String::compareTo);
                handleRedirection(files, redirectionFile, arguments[0]);
            } catch (IOException io){
                io.printStackTrace();
            }
        } else {
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

    private static void handleRedirection(List<String> files, String redirectionFile, String command){
        try(BufferedWriter bw = new BufferedWriter(new FileWriter(redirectionFile))){
            files.forEach(file -> {
                Path path = Paths.get(file);
                if(Files.exists(path) && Files.isReadable(path) && !Files.isDirectory(path)){
                    try(BufferedReader br = new BufferedReader(new FileReader(String.valueOf(path)))){
                        String line;
                        while((line = br.readLine()) != null){
                            bw.write(line);
                            bw.newLine();
                        }
                    } catch (IOException io){
                        io.printStackTrace();
                    }
                } else {
                    System.err.println(command + ": " + file + ": No such file or directory");
                }
            });
        } catch (IOException io){
            io.printStackTrace();
        }
    }
}

