package edu.umich.si.inteco.tansuo.app.model;

import java.util.ArrayList;


public class Task {
	
	private int _id=-1;
	private long _createdTime=-1;
	private int _studyId = -1;//know which study owns this probe Object
	private long _startTime=-1;
	private long _endTime=-1; 
	private String mName="";
	private String mDescription="";

	/**Session List**/
	public ArrayList<Session> mSessionList;
	
	/**Event List**/
	public ArrayList<Event> mEventList;

	public Task(int id ){
		_id = id;
	}
	
	public Task (int id, String name, String description, int study_id){
		
		_id = id;
		_studyId = study_id;
		mName = name;
		mDescription = description;
		mSessionList = new ArrayList<Session>();
	}
	
	public Task(int id, String name, long startTime, long endTime, String description, int study_id){

		_id = id;
		_studyId = study_id;
		_startTime = startTime;
		_endTime = endTime;
		mName = name;
		mDescription = description;
		mSessionList = new ArrayList<Session>();
		mEventList = new ArrayList<Event>();
	}

	public void setId(int id){
		_id = id;
	}

	public int getId(){
		return _id;
	}
	
    public void setSessionList(ArrayList<Session> sessionList ){
    	
    	mSessionList = sessionList;
    }
    
    public ArrayList<Session> getSessionList(){
    	
    	return mSessionList;
    }

    public void setEventList(ArrayList<Event> EventList ){
    	
    	mEventList = EventList;
    }
    
    public void addSession(Session session){
    	
    	mSessionList.add(session);
    }
    
    
    public ArrayList<Event> getEventList(){
    	
    	return mEventList;
    }
	
    public void addEvent(Event event){  
    	
    	mEventList.add(event);
    	event.setTask(this);
    	
    }
    
	public void setStartTime(long t){
		_startTime = t;
	}

	public long getStartTime(){
		return _startTime;
	}
	
	public void setEndTime(long t){
		_endTime = t;
	}

	public long getEndTime(){
		return _endTime;
	}
	
	public String getName(){
		return mName;
	} 
	
	public void setName(String n){
		mName = n;
	} 
	
	public String getDescription(){
		return mDescription;
	} 
	
	public void setDescription(String d){
		mDescription = d;
	} 
	
	public int getStudyId (){
		return _studyId;
	}
	
	public void setStudyId (int id){
		_studyId = id;
	}
	
	
}
