import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConfigParser {

	public static ProjectMain readConfigFile(String name) throws IOException{
		ProjectMain mySystem = new ProjectMain();
		int count = 0,flag = 0;
		// Variable to keep track of the current node whose neighbors are being updated
		int curNode = 0;
		// The name of the file to open.
		String curDir = System.getProperty("user.dir");
		String fileName = curDir+"/"+name;
		// This will reference one line at a time
		String line = null;
		try {
			// FileReader reads text files in the default encoding.
			FileReader fileReader = new FileReader(fileName);
			// Always wrap FileReader in BufferedReader.
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			while((line = bufferedReader.readLine()) != null) {
				if(line.length() == 0)
					continue;
				// Ignore comments and consider only those lines which are not comments
				if(!line.startsWith("#")){
					if(line.contains("#")){
						String[] input = line.split("#.*$");
						String[] input1 = input[0].split("\\s+");
						if(flag == 0 && input1.length == 3){
							mySystem.N = Integer.parseInt(input1[0]);
							mySystem.K = Integer.parseInt(input1[1]);
							mySystem.tobSendDelay = Integer.parseInt(input1[2]);							
							flag++;
						}
						else if(flag == 1 && count < mySystem.N)
						{							
							mySystem.nodes.add(new Node(Integer.parseInt(input1[0]),input1[1],Integer.parseInt(input1[2])));
							count++;
							if(count == mySystem.N){
								flag = 2;
							}
						}
					}
					else {
						String[] input = line.split("\\s+");
						if(flag == 0 && input.length == 3){
							mySystem.N = Integer.parseInt(input[0]);
							mySystem.K = Integer.parseInt(input[1]);
							mySystem.tobSendDelay = Integer.parseInt(input[2]);
							flag++;
						}
						else if(flag == 1 && count < mySystem.N)
						{
							mySystem.nodes.add(new Node(Integer.parseInt(input[0]),input[1],Integer.parseInt(input[2])));
							count++;
							if(count == mySystem.N){
								flag = 2;
							}
						}
					}
				}
			}
			// Always close files.
			bufferedReader.close();  
		}
		catch(FileNotFoundException ex) {
			System.out.println("Unable to open file '" +fileName + "'");                
		}
		catch(IOException ex) {
			System.out.println("Error reading file '" + fileName + "'");                  
		}
		return mySystem;
	}
	public static void main(String[] args) throws IOException{
		ProjectMain m = ConfigParser.readConfigFile("config1.txt");
		for(int i=0;i<m.nodes.size();i++){
			System.out.println(m.nodes.get(i));
		}
	}
}

