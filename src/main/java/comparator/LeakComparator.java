package comparator;

import leaks.Leak;

import java.util.*;

/**
 * This class is responsible for comparing Leak objects with each other. It provides means to sort the leaks according
 * to class name, method name and then sink method name. Furthermore, it provides methods to compare two Leak objects
 * according to class, method and sink method name. Finally, it allows to group Leaks by first sorting them, then
 * comparing the class, method and sink. If the Leaks are similar, they get grouped in a single Leak object.
 *
 * It does use the Leak class to sort and compare Leak objects. This class is being used by the AnalyseRunner object.
 *
 * @author Timo Spring
 */
public class LeakComparator {

    /**
     * Compares two Leak objects according to Class name of the Leak. It does so using Comparator.
     */
    Comparator<Leak> sortByClassName
            = (leak1, leak2) -> leak1.getClassName().compareToIgnoreCase(leak2.getClassName());

    /**
     * Compares two Leak objects according to method name of the Leak. It does so using Comparator.
     */
    Comparator<Leak> sortByMethodName
            = (leak1, leak2) -> leak1.getMethodName().compareToIgnoreCase(leak2.getMethodName());

    /**
     * Compares two Leak objects according to sink method name of the Leak. It does so using Comparator.
     */
    Comparator<Leak> sortBySinkMethod
            = (leak1, leak2) -> leak1.getSinkMethod().compareToIgnoreCase(leak2.getSinkMethod());


    /**
     * Sorts an ArrayList of Leak objects first by class name, then method name and lastly by sink method name. It
     * returns the sorted list of Leaks. It is being used to as a preparation to group similar leaks.
     *
     * @param leaks - ArrayList of Leak objects
     * @return a sorted ArrayList of Leak objects.
     */
    private ArrayList<Leak> sortLeaks(ArrayList<Leak> leaks){
        ArrayList<Leak> sortedLeaks = new ArrayList<>();
        leaks
                .stream()
                .sorted(
                       sortByClassName
                               .thenComparing(sortByMethodName)
                               .thenComparing(sortBySinkMethod)
                )
                .forEach(leak -> sortedLeaks.add(leak));

        return sortedLeaks;
    }


    /**
     * Groupes similar leaks by first sorting them according to class, method and sink method name. Then we iterate the
     * sorted list of Leak objects and compare the current Leak with the next Leak object in the list. If they show
     * the same class, method and sink method name, then the Leaks are similar and need to be grouped. We group Leaks
     * by merging the information from both Leak objects in the current Leak object. Such information are return values
     * (that not all tools are providing) or the list of tools that detected the leaks. After merging the information,
     * we proceed with the next Leak object.
     *
     * @param leaks - ArrayList of Leak objects
     * @return ArrayList of Leak objects that is sorted and where similar Leaks are grouped together in single Leaks
     */
    public ArrayList<Leak> groupLeaks(ArrayList<Leak> leaks){
        if(leaks.isEmpty()){return leaks;}
        leaks = sortLeaks(leaks);
        ArrayList<Leak> groupedLeaks = new ArrayList<>();
        Iterator<Leak> iterator = leaks.iterator();
        int i = 0;
        groupedLeaks.add(leaks.get(i));
        iterator.next();
        while(iterator.hasNext()){
            Leak currentLeak = groupedLeaks.get(i);
            Leak nextLeak = iterator.next();

            if(currentLeak.compareTo(nextLeak) == 0){
                currentLeak.enhanceFields(nextLeak);
            }else {
                groupedLeaks.add(nextLeak);
                i++;
            }
        }

        return groupedLeaks;
    }

}




