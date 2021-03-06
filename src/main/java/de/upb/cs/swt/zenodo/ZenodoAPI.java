package de.upb.cs.swt.zenodo;

import java.io.IOException;
import java.util.List;

public interface ZenodoAPI {

	public boolean test();
	public Deposition getDeposition(Integer id);
	public List<Deposition> getDepositions();
	public Deposition updateDeposition(Deposition deposition);
	public void deleteDeposition(Integer id);
	public Deposition createDeposition(final Metadata m) throws UnsupportedOperationException, IOException ;
	public List<DepositionFile> getFiles(Integer depositionId);
	public DepositionFile uploadFile(final FileMetadata f, Integer depositionId) throws UnsupportedOperationException, IOException;
	public boolean discard(Integer id);
	
}
