# AutoConnect

AutoConnect is a software service which facilitates real-time sensory communication and early warning prediction for autonomous and semi-autonomous vehicles. With AutoConnect loaded onto their firmware, these autonomous vehicles are capable of extending their range of sensory vision and enhancing any innate prediction mechanisms via the AutoConnect network, which utliizes a proprietary connection algorithm. Specifically, this connection algorithm considers vehicle speed, trajectory, location, as well as environmental factors including potential hazard zones, road-side stops, and traffic slow-downs, to determine appropriate connection candidates.

Essentially, vehicles on AutoConnect act likes nodes on a network. When a vehicle wants to connect to another car, it simply pings the central server by sending its unique ID and GPX info (e.g. location, speed, direction). The server runs the connection algorithm to determine a suitable car for the connection, and sends its ID back to the vehicle. Using this ID, the vehicle simply establishes an ad-hoc connection with the target car, and receives sensory info from the other car.

---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

*This repository hosts the source code for the actual simulation: a virtual run of vehicles around a specific region of Kingston, Ontario, constantly requesting vehicles to connect to and appropriately adjusting their trajectories and speed. The source code for the central server and the proprietary connection algorithm, as well as the visuals of the simulation, are hosted in another repository. This 4th-year Computer Engineering capstone project **won 3rd place at Queen's University (2020)**, as voted by fellow Electrical and Computer Engineering students.*

AutoConnect founders: Sudhir Pokhrel, John Bowden, Drew Harshaw


