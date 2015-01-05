package types;


	public class Paper {
		private String ID;
		private String title;
		private String venue;
		private String abs;
		private String citation;
		private int year;
		public void setAbs(String abs)
		{
			this.abs=abs;
		}
		public String getAbs()
		{
			return abs;
			}
		public void setID(String ID)
		{
			this.ID=ID;
		}
		public String getID()
		{
			return ID;
			}
		public void setTitle(String title)
		{
			this.title=title;
			}
		public String getTitle()
		{
			return title;
			}
		public void setVenue(String venue)
		{
			this.venue=venue;
			}
		public String getVenue()
		{
			return venue;
			}
		
		public void setCitation(String citation)
		{
			this.citation=citation;
			}
		public String getCitation()
		{
			return citation;
			}
		
		public void setYear(int year)
		{
			this.year=year;
			}
		public int getYear()
		{
			return year;
			}
		
		public String toString()
		{
			return ID+" "+title+" "+venue+" "+year+" "+citation;
		}
		public void clone(Paper paper)
		{
			ID=paper.getID();
			title=paper.getTitle();
			venue=paper.getVenue();
			year=paper.getYear();
			abs=paper.getAbs();
			citation=paper.getCitation();
		}
		
		
	}



