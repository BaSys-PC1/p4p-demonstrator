package de.dfki.cos.basys.p4p.controlcomponent.smartwatch;

import java.util.concurrent.TimeUnit;

import org.apache.thrift.TException;
import org.mockito.Mockito;

import de.dfki.cos.basys.controlcomponent.ExecutionCommand;
import de.dfki.cos.basys.controlcomponent.ExecutionMode;
import de.dfki.cos.basys.controlcomponent.ParameterDirection;
import de.dfki.cos.basys.controlcomponent.annotation.OperationMode;
import de.dfki.cos.basys.controlcomponent.annotation.Parameter;
import de.dfki.cos.basys.controlcomponent.impl.BaseControlComponent;
import de.dfki.cos.basys.controlcomponent.impl.BaseOperationMode;
import de.dfki.iui.hrc.hybritcommand.CommandState;

@OperationMode(name = "displayInfoMessage", shortName = "DIM", description = "mode for displaying a string based message on the display of the smartwatch", 
		allowedCommands = {	ExecutionCommand.HOLD, ExecutionCommand.RESET, ExecutionCommand.START, ExecutionCommand.STOP }, 
		allowedModes = { ExecutionMode.PRODUCTION, ExecutionMode.SIMULATION })
public class DisplayInfoMessageOperationMode extends BaseOperationMode<NotificationService> {

	@Parameter(name = "message", direction = ParameterDirection.IN)
	private String message = "";
	
	private NotificationService service;
	
	@Override
	protected void configureServiceMock(NotificationService serviceMock) {
		try {
			Mockito.when(serviceMock.displayInfoMessage(message)).thenReturn(CommandState.ACCEPTED);
			Mockito.when(service.getCommandState(Mockito.anyString())).thenReturn(CommandState.FINISHED);
		} catch (TException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	public DisplayInfoMessageOperationMode(BaseControlComponent component) {
		super(component);
		service = getService(NotificationService.class);
	}


	@Override
	public void onResetting() {
		message = "";	
	}

	@Override
	public void onStarting() {
		boolean messageSent = false;
		
		while(!messageSent) {
			try {
				service.displayInfoMessage(message);
				messageSent = true;
			} catch (TException e) {
				LOGGER.warn(" Display ressource not responding. Retrying ...");
				sleep(1500);
				service.reconnect();
			}
		}
	}

	@Override
	public void onExecute() {
		boolean executing = true;
		CommandState state = CommandState.EXECUTING;
		
		while(executing) {
			try {
				state = service.getCommandState("");
			} catch (TException e) {
				LOGGER.warn(" Display ressource not responding. Retrying ...");
				sleep(1500);
				service.reconnect();
				continue;
			}
			
			switch(state) {
			case EXECUTING:
				// Wait for completion
				break;
			case ABORTED:
				component.setErrorStatus(1, "aborted");
				component.stop(component.getOccupierId());
				executing = false;
				break;
			case FINISHED:
				executing = false;
				break;
			case PAUSED:
				// TODO should the component be hold here?
				break;
			default:
				LOGGER.warn("Received unexpected CommandState {} during task execution!", state);
				break;
			}
			sleep(500);
		}

	}

	@Override
	public void onCompleting() {
	}

	@Override
	public void onStopping() {
	}

	public void sleep(long ms) {
		try {
			TimeUnit.MILLISECONDS.sleep(ms);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
