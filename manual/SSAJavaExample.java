public class SSAJavaExample
{
    public static void main(String args[])
    {
        // create a console logger (to write toolbox's messages to the console)
        ssatoolbox.ConsoleLogger cl = new ssatoolbox.ConsoleLogger();

        // create instance of SSA main class: No GUI, use console logger
        ssatoolbox.Main ssaMain = new ssatoolbox.Main(false, cl);

        // load data from CSV-file
        ssaMain.loadTimeseries(new java.io.File("data.csv"));

        // set SSA parameters (here only the number of stationary sources)
        ssaMain.parameters.setNumberOfStationarySources(3);

        // set epochization type (here: use heuristic)
        ssaMain.data.setEpochType(ssatoolbox.Data.EPOCHS_EQUALLY_HEURISTIC);

        // NOW: Run SSA (*not* using a seperate thread!)
        ssaMain.runSSA(false);

        // save stationary sources
        ssaMain.saveStationarySourcesCSV(new java.io.File("stationary.csv"));

        // save non-stationary sources
        ssaMain.saveNonstationarySourcesCSV(new java.io.File("non-stationary.csv"));
    }
}