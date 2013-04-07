package powercrystals.minefactoryreloaded.circuits;

import powercrystals.minefactoryreloaded.api.rednet.IRedNetLogicCircuit;

public class Noop implements IRedNetLogicCircuit
{
	@Override
	public int getInputCount()
	{
		return 0;
	}

	@Override
	public int getOutputCount()
	{
		return 0;
	}

	@Override
	public int[] recalculateOutputValues(long worldTime, int[] inputValues)
	{
		return new int[0];
	}

}
