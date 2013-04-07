package powercrystals.minefactoryreloaded.tile;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;

import org.bouncycastle.util.Arrays;

import powercrystals.minefactoryreloaded.MineFactoryReloadedCore;
import powercrystals.minefactoryreloaded.api.rednet.IRedNetLogicCircuit;
import powercrystals.minefactoryreloaded.circuits.Noop;

public class TileEntityRedNetLogic extends TileEntity
{
	private class PinMapping
	{
		public PinMapping(int pin, int buffer)
		{
			this.pin = pin;
			this.buffer = buffer;
		}
		
		int pin;
		int buffer;
	}
	
	private IRedNetLogicCircuit[] _circuits = new IRedNetLogicCircuit[4];
	
	// 0 in, 1 internal, 2 out
	private int[][] _buffers = new int[3][];
	
	private PinMapping[][] _pinMappingInputs = new PinMapping[_circuits.length][];
	private PinMapping[][] _pinMappingOutputs = new PinMapping[_circuits.length][];
	
	public TileEntityRedNetLogic()
	{
		_buffers[0] = new int[16];
		_buffers[1] = new int[16];
		_buffers[2] = new int[16];
		
		_circuits[0] = new Noop();
		_pinMappingInputs[0] = new PinMapping[_circuits[0].getInputCount()];
		_pinMappingOutputs[0] = new PinMapping[_circuits[0].getOutputCount()];
		_circuits[1] = new Noop();
		_pinMappingInputs[1] = new PinMapping[_circuits[1].getInputCount()];
		_pinMappingOutputs[1] = new PinMapping[_circuits[1].getOutputCount()];
		_circuits[2] = new Noop();
		_pinMappingInputs[2] = new PinMapping[_circuits[2].getInputCount()];
		_pinMappingOutputs[2] = new PinMapping[_circuits[2].getOutputCount()];
		_circuits[3] = new Noop();
		_pinMappingInputs[3] = new PinMapping[_circuits[3].getInputCount()];
		_pinMappingOutputs[3] = new PinMapping[_circuits[3].getOutputCount()];
	}
	
	private IRedNetLogicCircuit getCircuit(String className)
	{
		try
		{
			return (IRedNetLogicCircuit) Class.forName(className).newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return new Noop();
		}
	}
	
	@Override
	public void updateEntity()
	{
		if(worldObj.isRemote)
		{
			return;
		}
		
		int[] lastOuput = Arrays.clone(_buffers[2]);
		
		for(int circuitNum = 0; circuitNum < _circuits.length; circuitNum++)
		{	
			int[] input = new int[_circuits[circuitNum].getInputCount()];
			for(int pinNum = 0; pinNum < input.length; pinNum++)
			{
				input[pinNum] = _buffers[_pinMappingInputs[circuitNum][pinNum].buffer][_pinMappingInputs[circuitNum][pinNum].pin];
			}
			
			int[] output = _circuits[circuitNum].recalculateOutputValues(worldObj.getTotalWorldTime(), input);

			for(int pinNum = 0; pinNum < output.length; pinNum++)
			{
				_buffers[_pinMappingOutputs[circuitNum][pinNum].buffer][_pinMappingOutputs[circuitNum][pinNum].pin] = output[pinNum];
			}
		}
		
		
		if(!Arrays.areEqual(lastOuput, _buffers[2]))
		{
			worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, MineFactoryReloadedCore.rednetLogicBlock.blockID);
		}
	}
	
	public int getOutputValue(int subnet)
	{
		return _buffers[2][subnet];
	}
	
	public int[] getOutputValues()
	{
		return _buffers[2];
	}
	
	public void onInputsChanged(int[] values)
	{
		_buffers[0] = values;
	}
	
	@Override
	public void writeToNBT(NBTTagCompound nbttagcompound)
	{
		super.writeToNBT(nbttagcompound);
		
		NBTTagList circuits = new NBTTagList();
		for(int c = 0; c < _circuits.length; c++)
		{
			NBTTagCompound circuit = new NBTTagCompound();
			circuit.setString("circuit", _circuits[c].getClass().getName());
			
			NBTTagList inputPins = new NBTTagList();
			for(int p = 0; p < _pinMappingInputs[c].length; p++)
			{
				NBTTagCompound pin = new NBTTagCompound();
				pin.setInteger("buffer", _pinMappingInputs[c][p].buffer);
				pin.setInteger("pin", _pinMappingInputs[c][p].pin);
				
				inputPins.appendTag(pin);
			}
			circuit.setTag("inputPins", inputPins);
			
			NBTTagList outputPins = new NBTTagList();
			for(int p = 0; p < _pinMappingOutputs[c].length; p++)
			{
				NBTTagCompound pin = new NBTTagCompound();
				pin.setInteger("buffer", _pinMappingOutputs[c][p].buffer);
				pin.setInteger("pin", _pinMappingOutputs[c][p].pin);
				
				outputPins.appendTag(pin);
			}
			circuit.setTag("outputPins", outputPins);
			
			circuits.appendTag(circuit);
		}
		
		nbttagcompound.setTag("circuits", circuits);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound nbttagcompound)
	{
		super.readFromNBT(nbttagcompound);
		
		NBTTagList circuits = nbttagcompound.getTagList("circuits");
		if(circuits != null)
		{
			for(int c = 0; c < circuits.tagCount(); c++)
			{
				_circuits[c] = getCircuit(((NBTTagCompound)circuits.tagAt(c)).getString("circuit"));
				_pinMappingInputs[c] = new PinMapping[_circuits[c].getInputCount()];
				_pinMappingOutputs[c] = new PinMapping[_circuits[c].getOutputCount()];
				
				NBTTagList inputPins = ((NBTTagCompound)circuits.tagAt(c)).getTagList("inputPins");
				if(inputPins != null)
				{
					for(int i = 0; i < inputPins.tagCount() && i < _pinMappingInputs[c].length; i++)
					{
						int pin = ((NBTTagCompound)inputPins.tagAt(i)).getInteger("pin");
						int buffer = ((NBTTagCompound)inputPins.tagAt(i)).getInteger("buffer");
						_pinMappingInputs[c][i] = new PinMapping(pin, buffer);
					}
				}
				
				NBTTagList outputPins = ((NBTTagCompound)circuits.tagAt(c)).getTagList("outputPins");
				if(outputPins != null)
				{
					for(int i = 0; i < outputPins.tagCount() && i < _pinMappingOutputs[c].length; i++)
					{
						int pin = ((NBTTagCompound)outputPins.tagAt(i)).getInteger("pin");
						int buffer = ((NBTTagCompound)outputPins.tagAt(i)).getInteger("buffer");
						_pinMappingOutputs[c][i] = new PinMapping(pin, buffer);
					}
				}
			}
		}
		
		/*_circuits[0] = getCircuit("powercrystals.minefactoryreloaded.circuits.And3");
		_pinMappingInputs[0] = new PinMapping[_circuits[0].getInputCount()];
		_pinMappingOutputs[0] = new PinMapping[_circuits[0].getOutputCount()];
		_pinMappingInputs[0][0] = new PinMapping(0, 0);
		_pinMappingInputs[0][1] = new PinMapping(2, 0);
		_pinMappingInputs[0][2] = new PinMapping(4, 0);
		_pinMappingOutputs[0][0] = new PinMapping(6, 2);
		
		_circuits[1] = getCircuit("powercrystals.minefactoryreloaded.circuits.Or3");
		_pinMappingInputs[1] = new PinMapping[_circuits[1].getInputCount()];
		_pinMappingOutputs[1] = new PinMapping[_circuits[1].getOutputCount()];
		_pinMappingInputs[1][0] = new PinMapping(1, 0);
		_pinMappingInputs[1][1] = new PinMapping(3, 0);
		_pinMappingInputs[1][2] = new PinMapping(5, 0);
		_pinMappingOutputs[1][0] = new PinMapping(7, 2);*/
	}
	
}
