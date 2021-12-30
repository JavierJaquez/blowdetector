# Blowdetector
An app used to perform a user study exploring the use of blowing as input in mobile applications. The input to classification  subsystem is a continuos recording from the phones microphone. The signal is transformed using the MFCC library wrote by chiachunfu (https://github.com/chiachunfu/speech/blob/master/speechandroid/src/org/tensorflow/demo/mfcc/MFCC.java). This serves as input to a Convolutional Neural Network, trained with TensorFlow. The output is a binary classification of wether a blow is detected or not. This classification runs continuosly, given a result approximatly every 50ms. This result is then used by another logic in the app, which represents the implementation of the user study.

# User Study
Two experiments were carried out.  The first was a Fitts’s law task that contained two conditions.  The baseline condition involved the use of the tapping method, thesecond condition used blowing.  The second experiment includeda reaction time task with 3 conditions.  In the first experiment, 4 degrees of difficulty in decreasing order were presented for each Fitts’s law task,each containing 18 trials.  The second experiment included 20 trials for each reactiontime tasks.

## Fitts's Law
<img src="https://github.com/JavierJaquez/blowdetector/blob/main/fitt-law-screenshot.jpg" width="15%" height="15%">

Here participants were presented witha circular arrangement of buttons and had to select the one highlighted. In on the experiments participants only had to click on the highlighted target. In the other experiment participants had to indicate with a click to the highlighted circle, and a blow to confirm the selection.

## Reaction Time Tasks

<img src="https://github.com/JavierJaquez/blowdetector/blob/main/reaction-time-screenshot.jpg" width="15%" height="15%">

We believe that a fair way of comparison between the blow and tapping methods is by measuring the speed at which a user performs a given task.  Given this, we cameup  with  a  reaction  time  tasks  with  three  conditions.   In  these  tasks,  participants were presented with a big square that occupied most of the screen.  The participantshad to select the square when its color changed to green.  After a successful selection, participants had to wait for a random delay, with a time between 1500 and 4000ms, before the square was highlighted in green again.  This procedure was repeated fora total of 20 selections.

The three different condition for the reaction time tasks are described below.

Blow:  in this condition,  participants had to blow in other to trigger a successful selection.   The  participants  held  their  phones  in  whichever  form  or  position  wascomfortable. 

Click to side:  participants were asked to hold their index finger on the side of thephone (where volume and power buttons are usually positioned).  They had to waitwith the finger in this location until the square was highlighted, then they had totap it and return to the same position.  This reaction time task was designed in thisway so that participants didn’t position their index finger directly above and closeto the target, therefore simulating a more realistic environment.

Click to hip:  we believe that one of the most significant advantages blowing hasover  tapping  is  when  the  phone  is  not  immediately  accessible.   To  simulate  thiscondition, we created the click to hip reaction time tasks.  In this task, participantshad to position their fingers near their hip.  Participants were asked to wait with their index finger in this location until the square was highlighted and return to thesame position after a successful selection.



