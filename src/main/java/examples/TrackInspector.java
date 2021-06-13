package examples;

import static examples.TrackerKeys.KEY_TRACKLET_LENGTH;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;
s
import fiji.plugin.trackmate.Spot;
import fiji.plugin.trackmate.Logger;
import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.SelectionModel;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import fiji.plugin.trackmate.TrackModel;
import fiji.plugin.trackmate.action.TrackMateAction;
import fiji.plugin.trackmate.gui.displaysettings.DisplaySettings;
import fiji.plugin.trackmate.tracking.SpotTracker;
import net.imglib2.util.Pair;
import net.imglib2.util.ValuePair;

public class TrackInspector implements TrackMateAction {



	
	@Override
	public void execute(TrackMate trackmate, SelectionModel selectionModel, DisplaySettings displaySettings,
			Frame parent) {
		
		Model model = trackmate.getModel();
		Settings settings = trackmate.getSettings();
		final TrackModel trackModel = model.getTrackModel();
		final SpotTracker tracker = trackmate.getSettings().trackerFactory.create(model.getSpots(), settings.trackerSettings);		
	   
		SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph = tracker.getResult();
		model.setTracks(graph, true);
        
		double timecutoff = 0;
		if (settings.trackerSettings.get(KEY_TRACKLET_LENGTH)!=null)
			timecutoff = (Double) settings.trackerSettings.get(KEY_TRACKLET_LENGTH);
		
		

		model.beginUpdate();

			for (final Integer trackID : trackModel.trackIDs(false)) {

				ArrayList<Pair<Integer, Spot>> Sources = new ArrayList<Pair<Integer, Spot>>();
				ArrayList<Pair<Integer, Spot>> Targets = new ArrayList<Pair<Integer, Spot>>();
				ArrayList<Integer> SourcesID = new ArrayList<Integer>();
				ArrayList<Integer> TargetsID = new ArrayList<Integer>();

				ArrayList<Pair<Integer, Spot>> Starts = new ArrayList<Pair<Integer, Spot>>();
				ArrayList<Pair<Integer, Spot>> Ends = new ArrayList<Pair<Integer, Spot>>();
				HashSet<Pair<Integer, Spot>> Splits = new HashSet<Pair<Integer, Spot>>();

				final Set<DefaultWeightedEdge> track = trackModel.trackEdges(trackID);

				for (final DefaultWeightedEdge e : track) {

					final double cost = model.getTrackModel().getEdgeWeight(e);
					Spot Spotbase = model.getTrackModel().getEdgeSource(e);
					Spot Spottarget = model.getTrackModel().getEdgeTarget(e);

					Integer targetID = Spottarget.ID();
					Integer sourceID = Spotbase.ID();
					Sources.add(new ValuePair<Integer, Spot>(sourceID, Spotbase));
					Targets.add(new ValuePair<Integer, Spot>(targetID, Spottarget));
					SourcesID.add(sourceID);
					TargetsID.add(targetID);

				}
				// find track ends
				for (Pair<Integer, Spot> tid : Targets) {

					if (!SourcesID.contains(tid.getA())) {

						Ends.add(tid);

					}

				}

				// find track starts
				for (Pair<Integer, Spot> sid : Sources) {

					if (!TargetsID.contains(sid.getA())) {

						Starts.add(sid);

					}

				}

				// find track splits
				int scount = 0;
				for (Pair<Integer, Spot> sid : Sources) {

					for (Pair<Integer, Spot> dupsid : Sources) {

						if (dupsid.getA().intValue() == sid.getA().intValue()) {
							scount++;
						}
					}
					if (scount > 1) {
						Splits.add(sid);
					}
					scount = 0;
				}

				if (Splits.size() > 0) {

					for (Pair<Integer, Spot> sid : Ends) {

						Spot Spotend = sid.getB();

						int trackletlength = 0;


						double minsize = Double.MAX_VALUE;
						Spot Actualsplit = null;
						for (Pair<Integer, Spot> splitid : Splits) {
							Spot Spotstart = splitid.getB();
							Set<Spot> spotset = connectedSetOf(graph, Spotend, Spotstart);
							

							if (spotset.size() < minsize) {

								minsize = spotset.size();
								Actualsplit = Spotstart;

							}

						}

						if (Actualsplit != null) {
							Set<Spot> connectedspotset = connectedSetOf(graph, Spotend, Actualsplit);
							trackletlength = (int) Math.abs(Actualsplit.diffTo(Spotend, Spot.FRAME));

							if (trackletlength <= timecutoff) {

								Iterator<Spot> it = connectedspotset.iterator();
							    while(it.hasNext())
							    	trackModel.removeSpot(it.next());

							}
						}

					}
				}
			}

		model.endUpdate();

	
	
	}

public Set<Spot> connectedSetOf(SimpleWeightedGraph<Spot, DefaultWeightedEdge> graph, Spot vertex, Spot split) {

	Set<Spot> connectedSet = new HashSet<>();

	connectedSet = new HashSet<>();

	BreadthFirstIterator<Spot,DefaultWeightedEdge > i = new BreadthFirstIterator<>(graph, vertex);

	do{
		Spot spot = i.next();
		 if(spot.ID() == split.ID()) {
			 break;
			 
		 }
		connectedSet.add(spot);
	}while (i.hasNext()); 


	return connectedSet;
}

@Override
public void setLogger(Logger logger) {
	// TODO Auto-generated method stub
	
}
	

}
