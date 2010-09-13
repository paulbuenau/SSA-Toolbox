package ssatoolbox;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class ToolboxConfig
{
    private final String CONFIG_DIR = ".ssa-toolbox";
    private final String CONFIG_FILE = "config";

    private File configFile;
    private Properties properties;

    public ToolboxConfig()
    {
        // get user home directory
        String userHome = System.getProperty("user.home");
        // get file separator
        String fileSep = System.getProperty("file.separator");
        configFile = new File(userHome + fileSep + CONFIG_DIR + fileSep + CONFIG_FILE);
        properties = new Properties();
        if(configFile.exists())
        {
            try
            {
                // load properties
                FileInputStream fis = new FileInputStream(configFile);
                properties.load(fis);
                fis.close();
            }
            catch (IOException ex) { }
        }
        else
        {
            // does config directory exists?
            File configDir = new File(userHome + fileSep + CONFIG_DIR);
            if(!configDir.exists())
            {
                // make directory
                configDir.mkdir();
            }
        }
    }

    public void setProperty(String Key, String Value)
    {
        properties.setProperty(Key, Value);
    }

    public String getProperty(String Key)
    {
        return properties.getProperty(Key);
    }

    public void saveProperties()
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(configFile);
            properties.store(fos, null);
        } catch (IOException ex) { }
    }
}
