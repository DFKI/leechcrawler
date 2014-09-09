// * Created on 04.11.2005
package de.dfki.km.leech.lucene;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;

import de.dfki.km.leech.lucene.FieldConfig.FieldMapping;
import de.dfki.km.leech.util.StringUtils;




public class LuceneAnalyzerFactory
{

    protected static Logger m_logger = Logger.getLogger(LuceneAnalyzerFactory.class.getName());




    /**
     * Creates a new Analyzer out of the given
     * 
     * @param fieldConfig a {@link FieldConfig}
     * 
     * @return the according analyzer
     * 
     * @throws Exception
     */
    public static Analyzer createAnalyzer(FieldConfig fieldConfig) throws Exception
    {

        String strDefaultAnalyzerName = fieldConfig.defaultFieldMapping.analyzer;
        Analyzer defaultAnalyzer = LuceneAnalyzerFactory.createAnalyzer(strDefaultAnalyzerName, null);


        HashMap<String, Analyzer> hsFieldName2Analyzer = new HashMap<String, Analyzer>();
        for (Entry<String, FieldMapping> fieldname2FieldMapping : fieldConfig.hsFieldName2FieldMapping.entrySet())
        {
            String strFieldName = fieldname2FieldMapping.getKey();
            String strAnalyzer4Field = fieldname2FieldMapping.getValue().analyzer;
            hsFieldName2Analyzer.put(strFieldName, LuceneAnalyzerFactory.createAnalyzer(strAnalyzer4Field, null));
        }

        return new PerFieldAnalyzerWrapper(defaultAnalyzer, hsFieldName2Analyzer);
    }



    /**
     * Creates a new <code>Analyzer</code>.
     * 
     * @param analyzerClassName The class name of the <code>Analyzer</code> to be created.
     * @param userGivenStopWordFileName The file name of the stop word file, or <code>null</code> or empty, if no stop words should be set. If the
     *            given file name is relative, it will be relative to the Catwiesel base directory (if specified via system property
     *            FileHandling.BASE_DIRECTORY_KEY or Catwiesel global configuration) or current working directory, if no Catwiesel base directory was
     *            specified.
     * 
     * @return the newly created analyzer
     * 
     * @throws Exception
     */
    public static Analyzer createAnalyzer(String analyzerClassName, String userGivenStopWordFileName) throws Exception
    {
        try
        {
            Analyzer analyzer;

            Class<?> analyzerClass = Class.forName(analyzerClassName);
            if(!StringUtils.nullOrWhitespace(userGivenStopWordFileName))
            {
                Class<?>[] parameterClasses = { String[].class };
                Constructor<?> constructor;
                try
                {
                    constructor = analyzerClass.getConstructor(parameterClasses);


                    m_logger.finer("creating Analyzer " + analyzerClassName + " with stopword file " + userGivenStopWordFileName);
                    InputStreamReader inReader = new InputStreamReader(new FileInputStream(userGivenStopWordFileName), "UTF-8");
                    BufferedReader reader = new BufferedReader(inReader);
                    ArrayList<String> wordList = new ArrayList<String>();
                    String stopWord = reader.readLine();
                    while (stopWord != null)
                    {
                        wordList.add(stopWord);
                        stopWord = reader.readLine();
                    }
                    reader.close();
                    String[] stopWords = wordList.toArray(new String[wordList.size()]);



                    Object[] parameters = { stopWords };
                    analyzer = (Analyzer) constructor.newInstance(parameters);
                }
                catch (NoSuchMethodException e)
                {
                    m_logger.warning("Analyzer '" + analyzerClassName
                            + "' cannot be parameterized with stop word list. Specified stop word list will be ignored");
                    constructor = analyzerClass.getConstructor(new Class[0]);
                    Object[] parameters = {};
                    analyzer = (Analyzer) constructor.newInstance(parameters);
                }

            }
            else
            {
                m_logger.finer("creating Analyzer " + analyzerClassName + " without stopword file");
                analyzer = (Analyzer) analyzerClass.newInstance();
            }

            return analyzer;

        }
        catch (Exception e)
        {
            m_logger.log(Level.WARNING, "Unable to instantiate Analyzer '" + analyzerClassName + "'.", e);
            throw new Exception("Unable to instantiate Analyzer '" + analyzerClassName + "'.", e);
        }
    }

}
