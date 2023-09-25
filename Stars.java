import java.util.*;
import java.io.*;
import java.awt.*;  
import java.awt.geom.Line2D;
import java.awt.geom.*;
import javax.swing.*;

public class Stars
{
    public static void main(String[] args) throws IOException
    {
        try {
            File galaxyFile = new File(args[0]);
            BufferedReader br = new BufferedReader(new FileReader(galaxyFile));
            int startIndex = Integer.parseInt(args[1]);
            int goalIndex = Integer.parseInt(args[2]);
            double distance = Double.parseDouble(args[3]);

            //Check that arguments are valid
            if (startIndex < 1)
            {
                System.err.println("Start Index is less than 1");
                System.exit(0);
            }
            if (goalIndex < 1)
            {
                System.err.println("Goal Index is less than 1");
                System.exit(0);
            }
            if (goalIndex == startIndex)
            {
                System.err.println("Goal cannot be start star");
                System.exit(0);
            }
            if (distance  <= 0)//If the distance is less than or equal to zero, no path can exist
            {
                System.out.println("No path found");
                System.exit(0);
            }

            AstarAlgorithm alg = new AstarAlgorithm(br, startIndex, goalIndex, distance);
            System.out.println(alg.search());
        }
        catch(Exception e)
        {
            System.err.println(e);
            System.err.println("Please make sure input is formatted correctly");
        }  
    }

    public static class AstarAlgorithm
    {
        private ArrayList<Star> _stars = new ArrayList<Star>();
        private double _distance;
        private ArrayList<Star> _frontier = new ArrayList<Star>();
        private ArrayList<Star> _closedStars = new ArrayList<Star>();
        private Star _start;
        private Star _goal;

        public static class DisplayGraphics extends JPanel{
            private ArrayList<Star> _displayStars = new ArrayList<Star>();
            private ArrayList<Star> _closedStars = new ArrayList<Star>();
            private Star _end;
            private Star _beginning; 
            private boolean _drawn = false;

            public void paintComponent(Graphics g) {  
                setBackground(Color.BLACK);
                 

                g.setColor(Color.BLUE);
                //Draw the paths of all the evaluated stars
                for (Star star : _closedStars) {
                    if (star._parent != null)//Essentially check we are not trying to access the starts parent, which does not exist and will be null
                    {
                        g.drawLine((int)star._sX, (int)star._sY, (int)star._parent._sX, (int)star._parent._sY);
                    }
                }

                //Redraw the final path in a different colour
                Star s = _end;
                g.setColor(Color.YELLOW);
                while (s != _beginning)
                {
                    g.drawLine((int)s._sX, (int)s._sY, (int)s._parent._sX, (int)s._parent._sY);
                    s = s._parent;
                }

                //Draw all the stars
                g.setColor(Color.WHITE);
                for (Star star : _displayStars) {
                    g.fillOval((int)star._sX - 2, (int)star._sY - 2, 4, 4);
                }

                //Redraw the closed stars
                g.setColor(Color.GREEN);
                for (Star star : _closedStars) {
                    g.fillOval((int)star._sX - 2, (int)star._sY - 2, 4, 4);
                }

                //Redraw the start and end stars
                g.setColor(Color.RED);
                g.fillOval((int)_beginning._sX - 2, (int)_beginning._sY - 2, 4, 4);
                g.fillOval((int)_end._sX - 2, (int)_end._sY - 2, 4, 4);
                _drawn = true;
            } 

            //Create the graphics object, which needs a list of all stars, evaluated stars, the start star, and the end star
            public DisplayGraphics(ArrayList<Star> stars, ArrayList<Star> closedStars, Star end, Star beginning)
            {
               _displayStars = stars;
               _closedStars = closedStars;
               _end = end;
               _beginning = beginning;

               //Scale the stars by multiplying their coordinates by 10. Flip the y axis
               for (Star s : _displayStars) {
                    s._sX = 20 + (s._sX * 10);
                    s._sY =  100 - s._sY;
                    s._sY = 20 + (s._sY * 10);
                } 
            }
            
        }
        
        private class Star{
            private int _sIndex;//The index of this star
            private double _sX;//The x coordinate of this star
            private double _sY;//The y coordinate of this star
            private double _fValue;//Calculated as the sum of the distance between the end star and the start star
            private double _goalDist;//The distance between this star and the goal star
            private Star _parent = null;//The star that was evaulated to 'find' this star
            private double _cost;//The cost is the distance from this star to its parent, plus the parent's cost
            public boolean _checked = false;//Used to mark if this star has been evaluated

            private ArrayList<Star> _neighbours = new ArrayList<Star>();//The edges of this star

            public Star(int index, double x, double y){
                _sIndex = index;
                _sX = x;
                _sY = y;
            }

            public void addNeighbour(Star s)//Add an edge to this star
            {
                _neighbours.add(s);
            }
        }

        public AstarAlgorithm(BufferedReader br, int startIndex, int goalIndex, double distance) throws IOException
        {
            _distance = distance;
            int counter = 1;
            String s = br.readLine();

            while (s!= null)//Read in the file, creating all of the stars, and specifying the start and end stars
            {
                String[] coordinates = s.split(",");
                Star star = new Star(counter, Double.parseDouble(coordinates[0]), Double.parseDouble(coordinates[1]));
                if (counter == startIndex)
                {
                    _start = star;
                    _start._cost = 0;
                    _frontier.add(_start);
                }
                else if (counter == goalIndex)
                {
                    _goal = star;
                }
                _stars.add(star);
                s = br.readLine();
                counter++;
            }

            //Calculate the distance from the goal for each star
            for (Star star : _stars) {
                star._goalDist = (double)Math.sqrt(Math.pow(star._sX - _goal._sX, 2) + (double)Math.pow(star._sY - _goal._sY, 2));
            }

            //If the start star or end star is not in the file, throw and error
            if (startIndex > counter || goalIndex > counter)
            {
                System.err.println("Either goal or star does not exist");
                System.exit(0);
            }
        }

        public void findEdges(Star s){
            for (Star star2 : _stars) {
                if (s != star2 && star2._checked == false)//The star we are comparing cannot have been evaluated and cannot be the same star
                {
                    //Calculate the distance between the two stars
                    double distance = (double)Math.sqrt(Math.pow(s._sX - star2._sX, 2) + (double)Math.pow(s._sY - star2._sY, 2));
                    if (_frontier.contains(star2))//If this star is in the frontier, do not re-add it. Just update the cost/f value and parent
                    {
                        double newCost = s._cost + distance;
                        double newFValue = newCost + star2._goalDist;
                        if (newFValue < star2._fValue)
                        {
                            star2._cost = s._cost + distance;
                            star2._fValue = star2._cost + star2._goalDist;
                            star2._parent = s;
                        }
                    }
                    else{//Otherwise add it to the frontier after calculating cost and f value
                        if (distance <= _distance)
                        {
                            star2._cost = s._cost + distance;
                            star2._fValue = star2._cost + star2._goalDist;
                            star2._parent = s;
                            s.addNeighbour(star2);
                        }
                    }
                }
            }  
        }

        public String search(){
            while (_frontier.size() != 0)//While the frontier is not empty, continue the search
            {   
                Star currentNode = _frontier.get(0);
                findEdges(currentNode);//Find the neighbours of this star
                for (Star neighbour : currentNode._neighbours) {//For each neighbour, insert it to the frontier
                    insertToFrontier(neighbour);
                }
                if (currentNode == _goal)//If this node is the goal, draw the graph and stop the search
                {
                    JFrame f=new JFrame();
                    f.setSize(1050,1050);
                    f.setBackground(Color.BLACK);
                    DisplayGraphics m = new DisplayGraphics(_stars, _closedStars, _goal, _start);  
                    f.add(m);  
                    f.setVisible(true); 
                    return printPath(currentNode);
                }
                _frontier.remove(currentNode);//Remove the current star from the frontier, as we have evaluated it
                _closedStars.add(currentNode);//Add it to closed stars, to know to draw it a different colour
                currentNode._checked = true;//Mark it as checked so we do not add it to the frontier again
            }
            return "No path was found";
        }

        public void insertToFrontier(Star s){
            int counter = 0;
            for (Star s2 : _frontier) {//Insert this star at a certain index so the frontier is sorted by f value
                if (s._fValue < s2._fValue)
                {
                    _frontier.add(counter, s);
                    return;
                }
                else if(s._fValue == s2._fValue){
                    if (s._goalDist < s2._goalDist)
                    {
                        _frontier.add(counter, s);
                        return;
                    }
                }
                counter++;
            }
            _frontier.add(s);
        }

        public String printPath(Star s)//print the path recursively
        {
            if (s == _start)
            {
                return "" + s._sIndex;
            }
            return "" +  printPath(s._parent)  + ", " + s._sIndex;
        }
    }
}