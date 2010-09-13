package ssatoolbox;

/**
 * ConsoleLogger.java
 *
 * @author Jan Saputra Mueller, saputra@cs.tu-berlin.de
 */
public class ConsoleLogger implements Logger
{
    public void appendToLog(String str)
    {
        System.out.println(str);
    }
}
